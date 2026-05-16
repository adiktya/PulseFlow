package com.pulseflow.workflow.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.pulseflow.workflow.domain.CampaignRepository
import com.pulseflow.workflow.service.CampaignTriggerService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class CampaignPipelineListeners(
    private val objectMapper: ObjectMapper,
    private val campaignRepository: CampaignRepository,
    private val campaignTriggerService: CampaignTriggerService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${pulseflow.kafka.topics.campaign-created}"],
        groupId = "pulseflow-workflow-campaign-created",
    )
    fun onCampaignCreated(payload: String) {
        val message = objectMapper.readValue(payload, CampaignCreatedMessage::class.java)
        val campaign =
            campaignRepository.findById(message.campaignId).orElseThrow {
                IllegalStateException("Campaign ${message.campaignId} not found")
            }

        val schedule = message.scheduleExpression?.trim()?.takeIf { it.isNotEmpty() }
        if (schedule == null) {
            log.info(
                "Campaign {} ({}) registered without schedule; enqueue jobs manually or add a cron later",
                campaign.id,
                campaign.name,
            )
            return
        }

        campaignTriggerService.validateSchedule(schedule)
        log.info(
            "Campaign {} ({}) registered for cron [{}]; scheduler will publish campaign.triggered when due",
            campaign.id,
            campaign.name,
            schedule,
        )
    }

    @KafkaListener(
        topics = ["\${pulseflow.kafka.topics.campaign-triggered}"],
        groupId = "pulseflow-workflow-campaign-triggered",
    )
    fun onCampaignTriggered(payload: String) {
        val message = objectMapper.readValue(payload, CampaignTriggeredMessage::class.java)
        campaignTriggerService.enqueueJobForTrigger(message)
    }
}
