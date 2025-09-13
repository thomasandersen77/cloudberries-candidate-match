package no.cloudberries.candidatematch.infrastructure.integration.embedding

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "embedding")
class EmbeddingConfig {
    var enabled: Boolean = false
    var provider: String = "GEMINI"
    var model: String = "text-embedding-004"
    var dimension: Int = 768
}