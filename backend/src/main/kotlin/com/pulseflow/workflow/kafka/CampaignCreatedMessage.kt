package com.pulseflow.workflow.kafka

import java.util.UUID

data class CampaignCreatedMessage(
    val campaignId: UUID,
    val name: String,
    val scheduleExpression: String?,
    val workflowId: String,
    val jobPayload: String,
)
