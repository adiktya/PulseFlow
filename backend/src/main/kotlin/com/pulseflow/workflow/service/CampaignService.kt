package com.pulseflow.workflow.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.pulseflow.workflow.api.CampaignResponse
import com.pulseflow.workflow.api.CreateCampaignRequest
import com.pulseflow.workflow.api.UpdateCampaignRequest
import com.pulseflow.workflow.domain.Campaign
import com.pulseflow.workflow.domain.CampaignRepository
import com.pulseflow.workflow.domain.CampaignStatus
import com.pulseflow.workflow.kafka.CampaignCreatedMessage
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class CampaignService(
    private val campaignRepository: CampaignRepository,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val campaignTriggerService: CampaignTriggerService,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun create(request: CreateCampaignRequest): CampaignResponse {
        val schedule = request.scheduleExpression?.trim()?.takeIf { it.isNotEmpty() }
        val workflowId = request.workflowId?.trim()?.takeIf { it.isNotEmpty() } ?: "kyc-reminder"
        val jobPayload = request.jobPayload?.trim()?.takeIf { it.isNotEmpty() } ?: """{"source":"campaign"}"""

        if (schedule != null) {
            campaignTriggerService.validateSchedule(schedule)
            validateJson(jobPayload)
        }

        val campaign =
            Campaign(
                name = request.name.trim(),
                scheduleExpression = schedule,
                workflowId = workflowId,
                jobPayload = jobPayload,
                status = CampaignStatus.ACTIVE,
            )
        val saved = campaignRepository.save(campaign)
        kafkaEventPublisher.publishCampaignCreated(
            CampaignCreatedMessage(
                campaignId = saved.id,
                name = saved.name,
                scheduleExpression = saved.scheduleExpression,
                workflowId = saved.workflowId,
                jobPayload = saved.jobPayload,
            ),
        )
        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun listRecent(): List<CampaignResponse> =
        campaignRepository.findTop200ByOrderByCreatedAtDesc().map { toResponse(it) }

    @Transactional(readOnly = true)
    fun get(id: UUID): CampaignResponse {
        val campaign = campaignRepository.findById(id).orElseThrow { notFound(id) }
        return toResponse(campaign)
    }

    @Transactional
    fun update(
        id: UUID,
        request: UpdateCampaignRequest,
    ): CampaignResponse {
        val campaign = campaignRepository.findById(id).orElseThrow { notFound(id) }
        if (campaign.status == CampaignStatus.ARCHIVED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update an archived campaign")
        }

        request.name?.trim()?.takeIf { it.isNotEmpty() }?.let { campaign.name = it }
        request.workflowId?.trim()?.takeIf { it.isNotEmpty() }?.let { campaign.workflowId = it }
        request.jobPayload?.trim()?.takeIf { it.isNotEmpty() }?.let {
            validateJson(it)
            campaign.jobPayload = it
        }
        request.status?.let { campaign.status = it }

        if (request.scheduleExpression != null) {
            val schedule = request.scheduleExpression.trim().takeIf { it.isNotEmpty() }
            if (schedule != null) {
                campaignTriggerService.validateSchedule(schedule)
            }
            if (schedule != campaign.scheduleExpression) {
                campaign.lastTriggeredAt = null
            }
            campaign.scheduleExpression = schedule
        }

        val saved = campaignRepository.save(campaign)
        return toResponse(saved)
    }

    /** Archives the campaign (stops cron) — jobs already created are kept. */
    @Transactional
    fun delete(id: UUID) {
        val campaign = campaignRepository.findById(id).orElseThrow { notFound(id) }
        campaign.status = CampaignStatus.ARCHIVED
        campaign.scheduleExpression = null
        campaignRepository.save(campaign)
    }

    private fun notFound(id: UUID): ResponseStatusException =
        ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign $id not found")

    private fun toResponse(campaign: Campaign): CampaignResponse =
        CampaignResponse(
            id = campaign.id,
            name = campaign.name,
            status = campaign.status,
            scheduleExpression = campaign.scheduleExpression,
            workflowId = campaign.workflowId,
            jobPayload = campaign.jobPayload,
            lastTriggeredAt = campaign.lastTriggeredAt,
        )

    private fun validateJson(payload: String) {
        try {
            objectMapper.readTree(payload)
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "jobPayload must be valid JSON")
        }
    }
}
