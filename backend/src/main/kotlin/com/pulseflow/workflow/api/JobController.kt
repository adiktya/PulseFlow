package com.pulseflow.workflow.api

import com.pulseflow.workflow.service.JobService
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/jobs")
@Validated
class JobController(
    private val jobService: JobService,
) {
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateJobRequest,
    ): JobResponse = jobService.create(request)

    @GetMapping
    fun list(): List<JobResponse> = jobService.listRecent()

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): JobResponse = jobService.get(id)

    @PostMapping("/{id}/replay")
    fun replay(
        @PathVariable id: UUID,
    ): JobResponse = jobService.replay(id)
}
