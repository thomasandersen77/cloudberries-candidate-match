package no.cloudberries.candidatematch.infrastructure.integration.anthropic

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.cloudberries.candidatematch.config.AnthropicProperties
import no.cloudberries.candidatematch.domain.ai.AIContentGenerator
import no.cloudberries.candidatematch.domain.ai.AIGenerationException
import no.cloudberries.candidatematch.domain.ai.AIResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

/**
 * HTTP client for Anthropic Claude API integration.
 * Implements AIContentGenerator using WebClient and Kotlin Coroutines.
 */
@Service
class AnthropicHttpClient(
    private val anthropicWebClient: WebClient,
    private val anthropicProperties: AnthropicProperties
) : AIContentGenerator {

    private val logger = KotlinLogging.logger {}

    fun testConnection(): Boolean {
        if (anthropicProperties.apiKey.isBlank()) {
            logger.error { "Anthropic API key not configured" }
            return false
        }
        return runCatching {
            val testPrompt = "Say 'yes' if you are available."
            val response = generateContent(testPrompt)
            response.content.lowercase().contains("yes")
        }.getOrElse {
            logger.error(it) {
                "Anthropic connection test failed. ${it.message}. Check model id '${anthropicProperties.model}'. " +
                        "Ensure it exists and is available."
            }
            false
        }
    }

    override fun generateContent(prompt: String): AIResponse {
        return runCatching {
            logger.info { "Requesting content generation from Anthropic. Model '${anthropicProperties.model}'" }
            val modelUsed = anthropicProperties.model.ifBlank { "claude-3-5-sonnet-20241022" }
            val content = runBlocking {
                fetchAndCleanAnthropicResponse(prompt)
            }
            val response = AIResponse(
                content = content,
                modelUsed = modelUsed
            )
            logger.info { "Anthropic Model $modelUsed generated content successfully" }
            response
        }.getOrElse { e ->
            val errorMessage = "Failed to generate content with Anthropic"
            logger.error(e) { errorMessage }
            if (e is AIGenerationException) throw e
            throw AIGenerationException(
                errorMessage,
                e
            )
        }
    }

    private suspend fun fetchAndCleanAnthropicResponse(prompt: String): String {
        val modelId = anthropicProperties.model.ifBlank { "claude-3-5-sonnet-20241022" }
        
        val requestBody = AnthropicRequest(
            model = modelId,
            maxTokens = 4096,
            messages = listOf(
                AnthropicMessage(
                    role = "user",
                    content = prompt
                )
            )
        )

        val response = anthropicWebClient.post()
            .uri("/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(requestBody))
            .retrieve()
            .awaitBody<AnthropicResponse>()

        val content = response.content.firstOrNull()?.text
            ?: throw AIGenerationException("No response content received from Anthropic")

        return content.cleanJsonResponse()
    }

    private fun String.cleanJsonResponse(): String =
        replace(
            Regex("```(json)?"),
            ""
        ).trim()
}

/**
 * Request DTO for Anthropic API.
 */
private data class AnthropicRequest(
    val model: String,
    @JsonProperty("max_tokens")
    val maxTokens: Int,
    val messages: List<AnthropicMessage>
)

/**
 * Message DTO for Anthropic API.
 */
private data class AnthropicMessage(
    val role: String,
    val content: String
)

/**
 * Response DTO for Anthropic API.
 */
private data class AnthropicResponse(
    val content: List<AnthropicContent>
)

/**
 * Content DTO for Anthropic API response.
 */
private data class AnthropicContent(
    val type: String,
    val text: String
)
