package com.pulseflow.workflow.kafka

import java.time.Instant
import java.util.UUID

data class CampaignTriggeredMessage(
    val campaignId: UUID,
    val name: String,
    val workflowId: String,
    val jobPayload: String,
    val scheduledFireTime: Instant,
    val triggerSource: String = "scheduler",
)
