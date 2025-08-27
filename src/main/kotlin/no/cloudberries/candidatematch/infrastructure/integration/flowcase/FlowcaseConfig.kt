package no.cloudberries.candidatematch.infrastructure.integration.flowcase

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "flowcase")
class FlowcaseConfig {
    lateinit var baseUrl: String
    lateinit var apiKey: String
}