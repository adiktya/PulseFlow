package com.pulseflow.workflow.config

import com.flashcache.sdk.FlashCacheClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class FlashCacheClientConfig(
    private val properties: PulseFlowProperties,
) {
    @Bean(destroyMethod = "close")
    fun flashCacheClient(): FlashCacheClient {
        val fc = properties.flashcache
        val client =
            FlashCacheClient(
                fc.host,
                fc.port,
                Duration.ofSeconds(fc.readTimeoutSeconds),
            )
        client.connect()
        return client
    }
}
