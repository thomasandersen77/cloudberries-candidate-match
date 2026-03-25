package no.cloudberries.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for Anthropic AI integration.
 * Maps to anthropic.* properties in application.yaml.
 */
@Configuration
@ConfigurationProperties(prefix = "anthropic")
class AnthropicProperties {
    var apiKey: String = ""
    var model: String = "claude-3-5-sonnet-20241022"
    var baseUrl: String = "https://api.anthropic.com"
}
