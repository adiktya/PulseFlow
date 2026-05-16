package com.pulseflow.workflow.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JobRepository : JpaRepository<Job, UUID> {
    fun findTop200ByOrderByCreatedAtDesc(): List<Job>
}
