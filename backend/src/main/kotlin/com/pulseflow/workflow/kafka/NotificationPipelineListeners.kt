package com.pulseflow.workflow.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.pulseflow.workflow.service.JobNotificationProcessor
import com.pulseflow.workflow.service.KafkaEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import kotlin.math.min

@Component
class NotificationPipelineListeners(
    private val objectMapper: ObjectMapper,
    private val processor: JobNotificationProcessor,
    private val kafkaEventPublisher: KafkaEventPublisher,
) {
    @KafkaListener(
        topics = ["\${pulseflow.kafka.topics.notification-send}"],
        groupId = "pulseflow-workflow-send",
        concurrency = "3",
    )
    fun onNotificationSend(payload: String) {
        val message = objectMapper.readValue(payload, NotificationJobMessage::class.java)
        processor.handleSend(message)
    }

    @KafkaListener(
        topics = ["\${pulseflow.kafka.topics.notification-retry}"],
        groupId = "pulseflow-workflow-retry",
        concurrency = "3",
    )
    fun onNotificationRetry(payload: String) {
        val message = objectMapper.readValue(payload, NotificationJobMessage::class.java)
        val shift = maxOf(0, message.attempt - 1)
        val delayMs = min(MAX_BACKOFF_MS, BASE_BACKOFF_MS shl shift)
        if (delayMs > 0) {
            Thread.sleep(delayMs)
        }
        kafkaEventPublisher.publishNotificationSend(message)
    }

    companion object {
        private const val MAX_BACKOFF_MS = 60_000L
        private const val BASE_BACKOFF_MS = 500L
    }
}
