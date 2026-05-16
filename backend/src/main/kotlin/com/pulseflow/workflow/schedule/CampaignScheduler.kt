package com.pulseflow.workflow.schedule

import com.pulseflow.workflow.config.PulseFlowProperties
import com.pulseflow.workflow.domain.CampaignRepository
import com.pulseflow.workflow.domain.CampaignStatus
import com.pulseflow.workflow.service.CampaignTriggerService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CampaignScheduler(
    private val campaignRepository: CampaignRepository,
    private val campaignTriggerService: CampaignTriggerService,
    private val properties: PulseFlowProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${pulseflow.campaign.scheduler-interval-ms:60000}", initialDelay = 15000)
    fun pollScheduledCampaigns() {
        if (!properties.campaign.schedulerEnabled) {
            return
        }
        val now = Instant.now()
        val scheduled = campaignRepository.findByStatusAndScheduleExpressionIsNotNull(CampaignStatus.ACTIVE)
        var fired = 0
        for (campaign in scheduled) {
            try {
                if (campaignTriggerService.publishTriggerIfDue(campaign, now)) {
                    fired++
                }
            } catch (ex: Exception) {
                log.error("Failed to evaluate campaign {}: {}", campaign.id, ex.message, ex)
            }
        }
        if (fired > 0) {
            log.info("Campaign scheduler fired {} campaign(s) at {}", fired, now)
        }
    }
}
