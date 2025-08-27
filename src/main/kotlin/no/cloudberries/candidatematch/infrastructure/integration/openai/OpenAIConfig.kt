package no.cloudberries.candidatematch.infrastructure.integration.openai

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "openai")
class OpenAIConfig {
    lateinit var apiKey: String
    lateinit var model: String
    lateinit var assistantId: String
}