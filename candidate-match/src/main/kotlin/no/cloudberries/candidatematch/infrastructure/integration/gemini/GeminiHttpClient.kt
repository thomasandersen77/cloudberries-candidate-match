package no.cloudberries.candidatematch.infrastructure.integration.gemini

import com.google.genai.Client
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.ai.AIContentGenerator
import no.cloudberries.candidatematch.domain.ai.AIGenerationException
import no.cloudberries.candidatematch.domain.ai.AIResponse
import org.springframework.stereotype.Service

@Service
class GeminiHttpClient(
    private val geminiConfig: GeminiConfig
) : AIContentGenerator {
    private val logger = KotlinLogging.logger {}
    private val client: Client by lazy {
        Client.builder()
            .apiKey(geminiConfig.apiKey)
            .build()
    }

    fun testConnection(): Boolean {
        if (geminiConfig.apiKey.isBlank()) {
            logger.error { "Gemini API key not configured" }
            return false
        }
        val modelId = geminiConfig.model.ifBlank { "gemini-1.5-pro" }
        return runCatching {
            val response = client.models.generateContent(
                modelId,
                "are you up? answer yes or no",
                null
            )
            response?.text()?.lowercase()?.contains("yes") ?: false
        }.getOrElse {
            logger.error(it) {
                "Gemini connection test failed. Check model id '${geminiConfig.model}'. " +
                        "Ensure it exists and is available in your region/project."
            }
            false
        }
    }

    override fun generateContent(prompt: String): AIResponse {
        runCatching {
            logger.info { "Requesting content generation from Gemini. Model '${geminiConfig.model}" }
            val modelUsed = geminiConfig.model.ifBlank { "gemini-1.5-pro" }
            try {
                val content = fetchAndCleanGeminiResponse(prompt)
                return AIResponse(
                    content = content,
                    modelUsed = modelUsed
                )
            } catch (e: Exception) {
                val errorMessage = "Failed to generate content with Gemini"
                logger.error(e) { errorMessage }
                if (e is AIGenerationException) throw e
                throw AIGenerationException(
                    errorMessage,
                    e
                )
            }.also { logger.info { "Gemini Model ${modelUsed ?: "default"} generated content: $it" } }
        }.getOrElse { e ->
            throw AIGenerationException(
                "Failed to generate content with Gemini",
                e
            )
        }
    }

    private fun fetchAndCleanGeminiResponse(prompt: String): String {
        val modelId = geminiConfig.model.ifBlank { "gemini-1.5-pro" }
        return client.models
            .generateContent(
                modelId,
                prompt,
                null
            )
            ?.text()
            ?.cleanJsonResponse()
            ?: throw AIGenerationException("No response received from Gemini")
    }

    private fun String.cleanJsonResponse(): String =
        replace(
            Regex("```(json)?"),
            ""
        ).trim()
}