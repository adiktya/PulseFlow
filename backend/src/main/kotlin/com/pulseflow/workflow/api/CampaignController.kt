package com.pulseflow.workflow.api

import com.pulseflow.workflow.service.CampaignService
import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated
import java.util.UUID
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/campaigns")
@Validated
class CampaignController(
    private val campaignService: CampaignService,
) {
    @GetMapping
    fun list(): List<CampaignResponse> = campaignService.listRecent()

    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateCampaignRequest,
    ): CampaignResponse = campaignService.create(request)

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): CampaignResponse = campaignService.get(id)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCampaignRequest,
    ): CampaignResponse = campaignService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) {
        campaignService.delete(id)
    }
}
