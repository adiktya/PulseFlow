package com.pulseflow.workflow.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CampaignRepository : JpaRepository<Campaign, UUID> {
    fun findTop200ByOrderByCreatedAtDesc(): List<Campaign>

    fun findByStatusAndScheduleExpressionIsNotNull(status: CampaignStatus): List<Campaign>

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query(
        """
        UPDATE Campaign c
        SET c.lastTriggeredAt = :fireTime, c.updatedAt = :fireTime
        WHERE c.id = :id
          AND c.status = com.pulseflow.workflow.domain.CampaignStatus.ACTIVE
          AND c.scheduleExpression IS NOT NULL
          AND (c.lastTriggeredAt IS NULL OR c.lastTriggeredAt < :fireTime)
        """,
    )
    fun claimScheduledFire(
        id: UUID,
        fireTime: java.time.Instant,
    ): Int
}
