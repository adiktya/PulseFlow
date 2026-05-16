package com.pulseflow.workflow

import com.pulseflow.workflow.config.PulseFlowProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties(PulseFlowProperties::class)
@EnableScheduling
class PulseFlowApplication

fun main(args: Array<String>) {
    runApplication<PulseFlowApplication>(*args)
}
