package no.cloudberries.candidatematch.integration.gemini

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "gemini")
class GeminiConfig {
    lateinit var apiKey: String
    lateinit var model: String
}