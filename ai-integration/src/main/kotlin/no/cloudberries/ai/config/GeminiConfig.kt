package no.cloudberries.ai.config

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
@Configuration
@ConfigurationProperties(prefix = "gemini")
class GeminiProperties {
    var apiKey: String = ""
    var model: String = ""
    var matchingModel: String = ""
    var flashModel: String = ""
    var fileStoreName: String = ""
    var useFilesApi: Boolean = true
}

/**
 * Spring configuration for Gemini AI integration.
 * Provides WebClient bean configured for Gemini API calls.
 */
@Configuration
class GeminiConfig {

    companion object {
        const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com"
        val DEFAULT_TIMEOUT: Duration = Duration.ofMinutes(10)
        val UPLOAD_TIMEOUT: Duration = Duration.ofSeconds(120)
    }

    @Bean
    fun geminiWebClient(geminiProperties: GeminiProperties): WebClient {
        return WebClient.builder()
            .baseUrl(GEMINI_BASE_URL)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("x-goog-api-key", geminiProperties.apiKey)
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
            }
            .build()
    }
}
