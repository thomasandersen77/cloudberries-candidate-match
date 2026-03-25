package no.cloudberries.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ollama")
class OllamaConfig {
    var baseUrl: String = "http://localhost:11434"
    var model: String = "qwen2.5:14b"  // Default to available model
    var connectTimeoutSeconds: Long = 10
    var readTimeoutSeconds: Long = 180  // 3 minutes for CV scoring
    var writeTimeoutSeconds: Long = 30
}
