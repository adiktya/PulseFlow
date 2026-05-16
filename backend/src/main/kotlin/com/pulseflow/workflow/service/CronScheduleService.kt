package com.pulseflow.workflow.service

import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset

@Service
class CronScheduleService {
    fun parse(expression: String): CronExpression =
        try {
            CronExpression.parse(expression.trim())
        } catch (ex: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid cron expression: $expression", ex)
        }

    /**
     * Returns the next scheduled instant strictly after [anchor], or null if none.
     */
    fun nextFireAfter(
        expression: String,
        anchor: Instant,
    ): Instant? {
        val cron = parse(expression)
        return cron.next(anchor.atZone(ZoneOffset.UTC))?.toInstant()
    }

    /**
     * True when [now] is at or past the next fire time after the last trigger (or campaign creation).
     */
    fun isDue(
        expression: String,
        anchor: Instant,
        now: Instant,
    ): Boolean {
        val next = nextFireAfter(expression, anchor.minusMillis(1)) ?: return false
        return !next.isAfter(now)
    }
}
