package no.cloudberries.ai.infrastructure.integration.gemini

import com.google.genai.Client
import mu.KotlinLogging
import no.cloudberries.ai.config.GeminiProperties
import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.domain.AIResponse
import no.cloudberries.ai.domain.DefaultAIResponse
import no.cloudberries.ai.port.AiContentGenerationPort
import org.springframework.stereotype.Service

@Service
class GeminiHttpClient(
    private val geminiConfig: GeminiProperties
) {
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
        val modelId = geminiConfig.flashModel.ifBlank { "gemini-1.5-pro" }
        return runCatching {
            val response = client.models.generateContent(
                modelId,
                "are you up? answer yes or no",
                null
            )
            response?.text()?.lowercase()?.contains("yes") ?: false
        }.getOrElse {
            logger.error {
                "Gemini connection test failed. ${it.message}. Check model id '${geminiConfig.model}'. " +
                        "Ensure it exists and is available in your region/project."
            }
            false
        }
    }

    fun generateContent(prompt: String): AIResponse {
        // provider is ignored here as this is the Gemini implementation
        return runCatching {
            logger.info { "Requesting content generation from Gemini. Model '${geminiConfig.model}" }
            val modelUsed = geminiConfig.model.ifBlank { "gemini-1.5-pro" }
            try {
                val content = fetchAndCleanGeminiResponse(prompt)
                return DefaultAIResponse(
                    content = content,
                    modelUsed = modelUsed
                )
            } catch (e: Exception) {
                val errorMessage = "Failed to generate content with Gemini"
                logger.error(e) { errorMessage }
                throw RuntimeException(errorMessage, e)
            }.also { logger.info { "Gemini Model ${modelUsed ?: "default"} generated content: $it" } }
        }.getOrElse { e ->
            throw RuntimeException("Failed to generate content with Gemini", e)
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
            ?: throw RuntimeException("No response received from Gemini")
    }

    private fun String.cleanJsonResponse(): String =
        replace(
            Regex("```(json)?"),
            ""
        ).trim()
}
