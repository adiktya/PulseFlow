package com.pulseflow.workflow.api

import com.pulseflow.workflow.domain.CampaignStatus
import com.pulseflow.workflow.domain.JobStatus
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class CreateCampaignRequest(
    @field:NotBlank val name: String,
    val scheduleExpression: String? = null,
    val workflowId: String? = null,
    val jobPayload: String? = null,
)

data class UpdateCampaignRequest(
    val name: String? = null,
    val scheduleExpression: String? = null,
    val workflowId: String? = null,
    val jobPayload: String? = null,
    val status: CampaignStatus? = null,
)

data class CampaignResponse(
    val id: UUID,
    val name: String,
    val status: CampaignStatus,
    val scheduleExpression: String?,
    val workflowId: String,
    val jobPayload: String,
    val lastTriggeredAt: Instant?,
)

data class CreateJobRequest(
    @field:NotBlank val workflowId: String,
    @field:NotBlank val payload: String,
    val idempotencyKey: String? = null,
    val campaignId: UUID? = null,
)

data class JobResponse(
    val id: UUID,
    val workflowId: String,
    val status: JobStatus,
    val retryCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
