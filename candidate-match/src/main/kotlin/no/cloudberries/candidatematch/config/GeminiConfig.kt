package no.cloudberries.candidatematch.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * Configuration properties for Gemini AI integration.
 * Maps to gemini.* properties in application.yaml.
 */
@ConfigurationProperties(prefix = "gemini")
data class GeminiProperties(
    val apiKey: String,
    val model: String,
    val matchingModel: String,  // Separate model for batch candidate ranking
    val flashModel: String,
    val fileStoreName: String,
    val useFilesApi: Boolean = true  // Toggle between inline CVs and Files API
)

/**
 * Configuration properties for matching service.
 * Maps to matching.* properties in application.yaml.
 */
@ConfigurationProperties(prefix = "matching")
data class MatchingProperties(
    val provider: String = "GEMINI",
    val topN: Int = 10,
    val enabled: Boolean = true,
    val model: String = "gemini-2.0-flash-exp",  // Can be overridden to gemini-3.0-pro-preview
    val timeout: Long = 600000  // 10 minutes in milliseconds for batch matching
)

/**
 * Spring configuration for Gemini AI integration.
 * Provides WebClient bean configured for Gemini API calls.
 */
@Configuration
class GeminiConfig {

    companion object {
        const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com"
        val DEFAULT_TIMEOUT: Duration = Duration.ofMinutes(10)  // 10 minutes for batch matching
        val UPLOAD_TIMEOUT: Duration = Duration.ofSeconds(120)
    }

    /**
     * WebClient configured for Gemini API.
     * - Base URL: https://generativelanguage.googleapis.com
     * - Timeouts: 60s for standard calls, 120s for uploads
     * - Headers: Accept and Content-Type for JSON
     */
    @Bean
    fun geminiWebClient(geminiProperties: GeminiProperties): WebClient {
        return WebClient.builder()
            .baseUrl(GEMINI_BASE_URL)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("x-goog-api-key", geminiProperties.apiKey)
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) // 16MB for large CV files
            }
            .build()
    }
}
