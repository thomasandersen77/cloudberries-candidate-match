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
        if(geminiConfig.apiKey.isBlank()){
            logger.error { "Gemini API key not configured" }
            return false // Returner false hvis ikke konfigurert
        }

        return runCatching { // Bruk runCatching for å håndtere exceptions
            val response = client.models.generateContent(geminiConfig.model, "are you up? answer yes or no", null)
            response?.text()?.lowercase()?.contains("yes") ?: false // Håndterer null-respons
        }.getOrElse {
            logger.error(it) { "Gemini connection test failed" }
            false
        }
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
                throw AIGenerationException(
                    errorMessage,
                    e
                )
            }
        }.getOrElse { e ->
            throw AIGenerationException(
                "Failed to generate content with Gemini",
                e
            )
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
        replace(
            Regex("```(json)?"),
            ""
        ).trim()

}