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
        try {
            logger.debug { "Generating content with prompt: $prompt" }
            
            val response = client.models
                .generateContent(geminiConfig.model, prompt, null)
                ?.text()
                ?.cleanJsonResponse()
                ?: throw AIGenerationException("No response received from Gemini")

            return AIResponse(
                content = response,
                modelUsed = geminiConfig.model
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate content with Gemini" }
            throw AIGenerationException("Failed to generate content with Gemini", e)
        }
    }

    private fun String.cleanJsonResponse(): String =
        replace("```json", "")
            .replace("```", "")
            .trim()
}