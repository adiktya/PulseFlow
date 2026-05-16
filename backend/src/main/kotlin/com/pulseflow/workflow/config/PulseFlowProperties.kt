package com.pulseflow.workflow.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pulseflow")
data class PulseFlowProperties(
    val maxRetries: Int = 3,
    val lockTtlSeconds: Long = 30,
    val campaign: CampaignScheduler = CampaignScheduler(),
    val notifications: Notifications = Notifications(),
    val kafka: KafkaTopics = KafkaTopics(),
    val flashcache: FlashCache = FlashCache(),
) {
    data class Notifications(
        val ntfy: Ntfy = Ntfy(),
    ) {
        data class Ntfy(
            /** When true, successful jobs POST to ntfy.sh (see https://ntfy.sh). */
            val enabled: Boolean = false,
            val serverUrl: String = "https://ntfy.sh",
            /** Subscribe to this topic in the ntfy app; override per job via payload.ntfyTopic. */
            val topic: String = "silversoul_alerts",
            val accessToken: String? = null,
        )
    }
    data class CampaignScheduler(
        val schedulerEnabled: Boolean = true,
        val schedulerIntervalMs: Long = 60_000,
    )
    data class FlashCache(
        val host: String = "localhost",
        val port: Int = 7654,
        val readTimeoutSeconds: Long = 10,
    )

    data class KafkaTopics(
        val campaignCreated: String = "campaign.created",
        val campaignTriggered: String = "campaign.triggered",
        val notificationSend: String = "notification.send",
        val notificationRetry: String = "notification.retry",
        val notificationDlq: String = "notification.dlq",
        val notificationCompleted: String = "notification.completed",
    )
}
