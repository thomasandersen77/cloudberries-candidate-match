package no.cloudberries.candidatematch.health

import no.cloudberries.candidatematch.config.GeminiProperties
import no.cloudberries.candidatematch.infrastructure.integration.gemini.GeminiHttpClient
import no.cloudberries.candidatematch.infrastructure.integration.openai.OpenAIConfig
import no.cloudberries.candidatematch.infrastructure.integration.openai.OpenAIHttpClient
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service

// I HealthService.kt
interface AIHealthChecker {
    fun isConfigured(): Boolean
    fun isHealthy(): Boolean
}

@Service
class OpenAIHealthChecker(
    private val openAIConfig: OpenAIConfig,
    private val openAIHttpClient: OpenAIHttpClient
) : AIHealthChecker {

    override fun isConfigured(): Boolean =
        runCatching { openAIConfig.apiKey.isNotBlank() }
            .getOrElse {
                return false
            }

    @Timed
    override fun isHealthy(): Boolean =
        runCatching {
            openAIHttpClient.testConnection()
            return true
        }
        .getOrElse { return false }

}

@Service
class GeminiHealthChecker(
    private val geminiConfig: GeminiProperties,
    private val geminiHttpClient: GeminiHttpClient
) : AIHealthChecker {

    override fun isConfigured(): Boolean =
        runCatching { geminiConfig.apiKey.isNotBlank() }
            .getOrElse {
                return false
            }

    @Timed
    override fun isHealthy(): Boolean {
        return geminiHttpClient.testConnection()
    }
}
