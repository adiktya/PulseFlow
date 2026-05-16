package com.pulseflow.workflow.notification

import com.fasterxml.jackson.databind.JsonNode
import com.pulseflow.workflow.config.PulseFlowProperties
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.*

@Component
class NtfyNotificationSender(
    private val properties: PulseFlowProperties,
    restClientBuilder: RestClient.Builder,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = restClientBuilder.build()

    fun sendJobSuccess(
        jobId: UUID,
        workflowId: String,
        payload: JsonNode,
        topicOverride: String?,
    ) {
        val ntfy = properties.notifications.ntfy
        if (!ntfy.enabled) return

        val topic = topicOverride?.trim()?.takeIf { it.isNotEmpty() } ?: ntfy.topic
        if (topic.isBlank()) {
            log.warn("ntfy enabled but topic is blank; skipping notification for job {}", jobId)
            return
        }

        val userId = if (payload.hasNonNull("userId")) {
            payload.get("userId").asText()
        } else null

        val body = buildString {
            append("Job ")
            append(jobId)
            append(" completed")
            if (userId != null) {
                append(" for user ")
                append(userId)
            }
            append(".")
        }

        val url = "${ntfy.serverUrl.trimEnd('/')}/$topic"

        try {
            restClient.post()
                .uri(url)
                .contentType(MediaType.TEXT_PLAIN)
                .header("Title", "PulseFlow · $workflowId")
                .header("Tags", "white_check_mark")
                .header("Priority", "default")
                .header("Authorization", "Bearer ${ntfy.accessToken}") // ✅ key change
                .body(body)
                .retrieve()
                .toBodilessEntity()

            log.info("ntfy notification sent for job {} to topic {}", jobId, topic)
        } catch (ex: Exception) {
            throw IllegalStateException("ntfy publish failed: ${ex.message}", ex)
        }
    }
}