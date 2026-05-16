package com.pulseflow.workflow.kafka

import java.util.UUID

data class NotificationJobMessage(
    val jobId: UUID,
    val workflowId: String,
    val payload: String,
    val idempotencyKey: String? = null,
    val attempt: Int = 0,
)
