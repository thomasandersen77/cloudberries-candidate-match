package no.cloudberries.candidatematch.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * Spring configuration for Anthropic AI integration.
 * Provides WebClient bean configured for Anthropic API calls.
 */
@Configuration
class AnthropicConfig {

    companion object {
        val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(60)
    }

    /**
     * WebClient configured for Anthropic API.
     * - Base URL: https://api.anthropic.com (or from config)
     * - Timeout: 60s for standard calls
     * - Headers: Accept, Content-Type, x-api-key, anthropic-version
     */
    @Bean
    fun anthropicWebClient(anthropicProperties: AnthropicProperties): WebClient {
        return WebClient.builder()
            .baseUrl(anthropicProperties.baseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("x-api-key", anthropicProperties.apiKey)
            .defaultHeader("anthropic-version", "2023-06-01")
            .build()
    }
}
