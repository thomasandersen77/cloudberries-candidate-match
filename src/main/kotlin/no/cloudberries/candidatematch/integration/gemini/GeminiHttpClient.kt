package no.cloudberries.candidatematch.integration.gemini

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

    override fun generateContent(prompt: String): AIResponse {
        runCatching {
            logger.debug { "Requesting content generation from Gemini." }
            try {
                val content = fetchAndCleanGeminiResponse(prompt)
                return AIResponse(
                    content = content,
                    modelUsed = geminiConfig.model
                )
            } catch (e: Exception) {
                val errorMessage = "Failed to generate content with Gemini"
                logger.error(e) { errorMessage }
                // Avoid re-wrapping our specific exception.
                if (e is AIGenerationException) throw e
                throw AIGenerationException(errorMessage, e)
            }
        }.getOrElse { e->
            throw AIGenerationException("Failed to generate content with Gemini", e)
        }
    }

    private fun fetchAndCleanGeminiResponse(prompt: String): String {
        return client.models
            .generateContent(
                geminiConfig.model,
                prompt,
                null
            )
            ?.text()
            ?.cleanJsonResponse()
            ?: throw AIGenerationException("No response received from Gemini")
    }

    private fun String.cleanJsonResponse(): String =
        replace(Regex("```(json)?"), "").trim()

}