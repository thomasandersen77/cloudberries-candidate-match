package no.cloudberries.candidatematch.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "ai")
data class AIChatConfig(
    var provider: String = "GEMINI",
    var models: ModelsConfig = ModelsConfig(),
    var timeouts: TimeoutsConfig = TimeoutsConfig(),
    var rag: RagConfig = RagConfig(),
    var semantic: SemanticConfig = SemanticConfig(),
    var hybrid: HybridConfig = HybridConfig(),
    var streaming: StreamingConfig = StreamingConfig()
)

data class ModelsConfig(
    var interpretation: String = "gemini-1.5-flash-latest",
    var generationDefault: String = "gemini-1.5-flash-latest",
    var generationQuality: String = "gemini-1.5-pro-latest", 
    var embeddings: String = "text-embedding-004"
)

data class TimeoutsConfig(
    var interpretation: Long = 1500L, // ms
    var generation: Long = 3000L,     // ms  
    var retrieval: Long = 150L        // ms
)

data class RagConfig(
    var enabled: Boolean = true,
    var chunkSize: Int = 700,
    var chunkOverlap: Int = 100,
    var topKChunks: Int = 8
)

data class SemanticConfig(
    var enabled: Boolean = true
)

data class HybridConfig(
    var enabled: Boolean = true,
    var semanticWeight: Double = 0.6,
    var qualityWeight: Double = 0.4
)

data class StreamingConfig(
    var enabled: Boolean = false
)