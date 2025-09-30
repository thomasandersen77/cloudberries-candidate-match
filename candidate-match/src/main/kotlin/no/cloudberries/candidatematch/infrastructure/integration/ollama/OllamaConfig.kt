package no.cloudberries.candidatematch.infrastructure.integration.ollama

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ai.ollama")
data class OllamaConfig(
    var baseUrl: String = "http://localhost:11434",
    var model: String = "deepseek-r1:8b",
    var connectTimeoutSeconds: Long = 5,
    var readTimeoutSeconds: Long = 120,
    var writeTimeoutSeconds: Long = 10
)