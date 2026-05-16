package com.pulseflow.workflow.service

import com.pulseflow.workflow.api.CreateJobRequest
import com.pulseflow.workflow.domain.Campaign
import com.pulseflow.workflow.domain.CampaignRepository
import com.pulseflow.workflow.domain.CampaignStatus
import com.pulseflow.workflow.kafka.CampaignTriggeredMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class CampaignTriggerService(
    private val campaignRepository: CampaignRepository,
    private val cronScheduleService: CronScheduleService,
    private val jobService: JobService,
    private val kafkaEventPublisher: KafkaEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Claims the campaign for [fireTime] and publishes [campaign.triggered] so the consumer enqueues work.
     */
    @Transactional
    fun publishTriggerIfDue(
        campaign: Campaign,
        now: Instant,
    ): Boolean {
        val schedule = campaign.scheduleExpression?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        if (campaign.status != CampaignStatus.ACTIVE) {
            return false
        }

        val anchor = campaign.lastTriggeredAt ?: campaign.createdAt.minusMillis(1)
        if (!cronScheduleService.isDue(schedule, anchor, now)) {
            return false
        }

        val fireTime =
            cronScheduleService.nextFireAfter(schedule, anchor.minusMillis(1))
                ?: return false

        val claimed = campaignRepository.claimScheduledFire(campaign.id, fireTime)
        if (claimed == 0) {
            return false
        }

        kafkaEventPublisher.publishCampaignTriggered(
            CampaignTriggeredMessage(
                campaignId = campaign.id,
                name = campaign.name,
                workflowId = campaign.workflowId,
                jobPayload = campaign.jobPayload,
                scheduledFireTime = fireTime,
                triggerSource = "scheduler",
            ),
        )
        log.info("Published campaign.triggered for campaign {} at {}", campaign.id, fireTime)
        return true
    }

    @Transactional
    fun enqueueJobForTrigger(message: CampaignTriggeredMessage) {
        val campaign =
            campaignRepository.findById(message.campaignId).orElseThrow {
                IllegalStateException("Campaign ${message.campaignId} not found")
            }
        if (campaign.status != CampaignStatus.ACTIVE) {
            log.warn("Skipping trigger for non-active campaign {}", message.campaignId)
            return
        }

        val idempotencyKey =
            "campaign:${message.campaignId}:${message.scheduledFireTime.epochSecond}"

        jobService.create(
            CreateJobRequest(
                workflowId = message.workflowId,
                payload = message.jobPayload,
                idempotencyKey = idempotencyKey,
                campaignId = message.campaignId,
            ),
        )
        log.info(
            "Enqueued job for campaign {} (fire={}, source={})",
            message.campaignId,
            message.scheduledFireTime,
            message.triggerSource,
        )
    }

    fun validateSchedule(expression: String?) {
        val trimmed = expression?.trim()?.takeIf { it.isNotEmpty() } ?: return
        cronScheduleService.parse(trimmed)
    }
}
