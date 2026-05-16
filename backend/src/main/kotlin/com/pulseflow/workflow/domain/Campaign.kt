package com.pulseflow.workflow.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "campaigns")
class Campaign(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 512)
    var name: String,
    @Column(name = "schedule_expression")
    var scheduleExpression: String? = null,
    @Column(name = "workflow_id", nullable = false, length = 255)
    var workflowId: String = "kyc-reminder",
    @Column(name = "job_payload", nullable = false, columnDefinition = "text")
    var jobPayload: String = "{}",
    @Column(name = "last_triggered_at")
    var lastTriggeredAt: Instant? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: CampaignStatus = CampaignStatus.DRAFT,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @PrePersist
    fun onCreate() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
