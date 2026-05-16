package com.pulseflow.workflow.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.pulseflow.workflow.config.PulseFlowProperties
import com.pulseflow.workflow.kafka.CampaignCreatedMessage
import com.pulseflow.workflow.kafka.CampaignTriggeredMessage
import com.pulseflow.workflow.kafka.NotificationJobMessage
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val properties: PulseFlowProperties,
) {
    fun publishCampaignCreated(message: CampaignCreatedMessage) {
        send(properties.kafka.campaignCreated, message.campaignId.toString(), objectMapper.writeValueAsString(message))
    }

    fun publishCampaignTriggered(message: CampaignTriggeredMessage) {
        send(
            properties.kafka.campaignTriggered,
            message.campaignId.toString(),
            objectMapper.writeValueAsString(message),
        )
    }

    fun publishNotificationSend(message: NotificationJobMessage) {
        send(properties.kafka.notificationSend, message.jobId.toString(), objectMapper.writeValueAsString(message))
    }

    fun publishNotificationRetry(message: NotificationJobMessage) {
        send(properties.kafka.notificationRetry, message.jobId.toString(), objectMapper.writeValueAsString(message))
    }

    fun publishDlq(original: NotificationJobMessage, reason: String) {
        val payload =
            mapOf(
                "original" to original,
                "reason" to reason,
            )
        send(properties.kafka.notificationDlq, original.jobId.toString(), objectMapper.writeValueAsString(payload))
    }

    fun publishCompleted(jobId: UUID, workflowId: String) {
        val payload =
            mapOf(
                "jobId" to jobId,
                "workflowId" to workflowId,
            )
        send(properties.kafka.notificationCompleted, jobId.toString(), objectMapper.writeValueAsString(payload))
    }

    private fun send(topic: String, key: String, json: String) {
        kafkaTemplate.send(topic, key, json).get()
    }
}
