package com.pulseflow.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PulseGatewayApplication

fun main(args: Array<String>) {
    runApplication<PulseGatewayApplication>(*args)
}
