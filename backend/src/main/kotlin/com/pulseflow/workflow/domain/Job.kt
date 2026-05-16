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
@Table(name = "jobs")
class Job(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "workflow_id", nullable = false)
    var workflowId: String,
    @Column(nullable = false, columnDefinition = "text")
    var payload: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: JobStatus = JobStatus.QUEUED,
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,
    @Column(name = "idempotency_key")
    var idempotencyKey: String? = null,
    @Column(name = "campaign_id", columnDefinition = "uuid")
    var campaignId: UUID? = null,
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
