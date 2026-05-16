package com.pulseflow.workflow.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.pulseflow.workflow.config.PulseFlowProperties
import com.pulseflow.workflow.domain.JobRepository
import com.pulseflow.workflow.domain.JobStatus
import com.pulseflow.workflow.kafka.NotificationJobMessage
import com.pulseflow.workflow.notification.NtfyNotificationSender

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class JobNotificationProcessor(
    private val jobRepository: JobRepository,
    private val flashCacheCoordinationService: FlashCacheCoordinationService,
    private val objectMapper: ObjectMapper,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val ntfyNotificationSender: NtfyNotificationSender,
    private val properties: PulseFlowProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleSend(message: NotificationJobMessage) {
        val job = jobRepository.findById(message.jobId).orElseThrow()
        val idempotencyKey = job.idempotencyKey ?: job.id.toString()

        if (flashCacheCoordinationService.alreadyProcessed(idempotencyKey)) {
            log.debug("Skipping already processed job {}", message.jobId)
            return
        }

        if (!flashCacheCoordinationService.tryAcquireJobLock(message.jobId)) {
            log.info("Skipping job {} because lock not acquired", message.jobId)
            return
        }

        try {
            if (job.status == JobStatus.SUCCESS) {
                return
            }

            job.status = JobStatus.PROCESSING
            jobRepository.save(job)

            val payloadTree = simulateExecution(message)
            deliverNotification(message, payloadTree)

            job.status = JobStatus.SUCCESS
            jobRepository.save(job)

            flashCacheCoordinationService.markProcessed(idempotencyKey)
            kafkaEventPublisher.publishCompleted(job.id, message.workflowId)
        } catch (ex: Exception) {
            log.warn("Job {} failed on attempt {}: {}", message.jobId, message.attempt, ex.message)
            handleFailure(message, ex)
        } finally {
            flashCacheCoordinationService.releaseJobLock(message.jobId)
        }
    }

    private fun simulateExecution(message: NotificationJobMessage): com.fasterxml.jackson.databind.JsonNode {
        val tree = objectMapper.readTree(message.payload)
        val simulate = if (tree.hasNonNull("simulate")) tree.get("simulate").asText() else null
        if ("fail-always" == simulate) {
            throw IllegalStateException("simulated permanent failure")
        }
        if ("fail-once" == simulate && message.attempt == 0) {
            throw IllegalStateException("simulated transient failure")
        }
        return tree
    }

    private fun deliverNotification(
        message: NotificationJobMessage,
        payload: com.fasterxml.jackson.databind.JsonNode,
    ) {
        val topicOverride =
            if (payload.hasNonNull("ntfyTopic")) payload.get("ntfyTopic").asText() else null
        ntfyNotificationSender.sendJobSuccess(
            jobId = message.jobId,
            workflowId = message.workflowId,
            payload = payload,
            topicOverride = topicOverride,
        )
    }

    private fun handleFailure(message: NotificationJobMessage, ex: Exception) {
        val job = jobRepository.findById(message.jobId).orElseThrow()
        job.retryCount += 1
        if (job.retryCount >= properties.maxRetries) {
            job.status = JobStatus.FAILED
            jobRepository.save(job)
            kafkaEventPublisher.publishDlq(message, ex.message ?: "unknown")
        } else {
            jobRepository.save(job)
            kafkaEventPublisher.publishNotificationRetry(
                message.copy(attempt = message.attempt + 1),
            )
        }
    }
}
