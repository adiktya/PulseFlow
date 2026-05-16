package com.pulseflow.workflow.service

import com.pulseflow.workflow.api.CreateJobRequest
import com.pulseflow.workflow.api.JobResponse
import com.pulseflow.workflow.domain.Job
import com.pulseflow.workflow.domain.JobRepository
import com.pulseflow.workflow.domain.JobStatus
import com.pulseflow.workflow.kafka.NotificationJobMessage
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val flashCacheCoordinationService: FlashCacheCoordinationService,
) {
    @Transactional
    fun create(request: CreateJobRequest): JobResponse {
        val job =
            Job(
                workflowId = request.workflowId.trim(),
                payload = request.payload,
                status = JobStatus.QUEUED,
                idempotencyKey = request.idempotencyKey?.trim()?.takeIf { it.isNotEmpty() },
                campaignId = request.campaignId,
            )
        val saved = jobRepository.save(job)
        kafkaEventPublisher.publishNotificationSend(
            NotificationJobMessage(
                jobId = saved.id,
                workflowId = saved.workflowId,
                payload = saved.payload,
                idempotencyKey = saved.idempotencyKey,
                attempt = 0,
            ),
        )
        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun listRecent(): List<JobResponse> =
        jobRepository.findTop200ByOrderByCreatedAtDesc().map { toResponse(it) }

    @Transactional(readOnly = true)
    fun get(id: UUID): JobResponse {
        val job = jobRepository.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        return toResponse(job)
    }

    @Transactional
    fun replay(id: UUID): JobResponse {
        val job = jobRepository.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (job.status != JobStatus.FAILED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only FAILED jobs can be replayed")
        }
        flashCacheCoordinationService.clearProcessedMarkers(job)
        job.status = JobStatus.QUEUED
        job.retryCount = 0
        val saved = jobRepository.save(job)
        kafkaEventPublisher.publishNotificationSend(
            NotificationJobMessage(
                jobId = saved.id,
                workflowId = saved.workflowId,
                payload = saved.payload,
                idempotencyKey = saved.idempotencyKey,
                attempt = 0,
            ),
        )
        return toResponse(saved)
    }

    private fun toResponse(job: Job): JobResponse =
        JobResponse(
            id = job.id,
            workflowId = job.workflowId,
            status = job.status,
            retryCount = job.retryCount,
            createdAt = job.createdAt,
            updatedAt = job.updatedAt,
        )
}
