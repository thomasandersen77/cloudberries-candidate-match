package no.cloudberries.candidatematch.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for Anthropic AI integration.
 * Maps to anthropic.* properties in application.yaml.
 */
@ConfigurationProperties(prefix = "anthropic")
data class AnthropicProperties(
    val apiKey: String,
    val model: String = "claude-3-5-sonnet-20241022",
    val baseUrl: String = "https://api.anthropic.com"
)
