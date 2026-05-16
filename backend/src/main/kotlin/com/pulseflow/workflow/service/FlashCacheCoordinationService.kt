package com.pulseflow.workflow.service

import com.flashcache.sdk.FlashCacheClient
import com.pulseflow.workflow.config.PulseFlowProperties
import com.pulseflow.workflow.domain.Job
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.Duration
import java.util.UUID

@Service
class FlashCacheCoordinationService(
    private val flashCache: FlashCacheClient,
    private val properties: PulseFlowProperties,
) {
    fun tryAcquireJobLock(jobId: UUID): Boolean {
        val key = "pf:lock:job:${jobId}"
        return try {
            flashCache.setIfAbsent(key, "1", Duration.ofSeconds(properties.lockTtlSeconds))
        } catch (e: IOException) {
            throw IllegalStateException("FlashCache unavailable", e)
        }
    }

    fun releaseJobLock(jobId: UUID) {
        try {
            flashCache.delete("pf:lock:job:${jobId}")
        } catch (_: IOException) {
            // best-effort unlock
        }
    }

    fun markProcessed(idempotencyKey: String, ttlSeconds: Long = 86400L) {
        try {
            flashCache.set("pf:processed:${idempotencyKey}", "1", Duration.ofSeconds(ttlSeconds))
        } catch (e: IOException) {
            throw IllegalStateException("FlashCache unavailable", e)
        }
    }

    fun alreadyProcessed(idempotencyKey: String): Boolean =
        try {
            flashCache.exists("pf:processed:${idempotencyKey}")
        } catch (e: IOException) {
            throw IllegalStateException("FlashCache unavailable", e)
        }

    fun clearProcessedMarkers(job: Job) {
        try {
            flashCache.delete("pf:processed:${job.id}")
            job.idempotencyKey?.let { flashCache.delete("pf:processed:${it}") }
        } catch (_: IOException) {
            // best-effort
        }
    }
}
