package no.cloudberries.candidatematch.infrastructure.integration.gemini

import com.google.genai.Client
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.ai.AIContentGenerator
import no.cloudberries.candidatematch.domain.ai.AIGenerationException
import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.domain.ai.ChatContext
import no.cloudberries.candidatematch.domain.ai.Role
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
        val modelId = geminiConfig.quickModel.ifBlank { geminiConfig.model.ifBlank { "gemini-2.5-flash" } }
        return runCatching {
            val response = client.models.generateContent(
                modelId,
                "are you up? answer yes or no",
                null
            )
            response?.text()?.lowercase()?.contains("yes") ?: false
        }.getOrElse {
            logger.error(it) {
                "Gemini connection test failed. Check model id '$modelId'. Ensure it exists and is available in your region/project."
            }
            false
        }
    }

    override fun generateContent(prompt: String): AIResponse {
        runCatching {
            val modelUsed = geminiConfig.quickModel.ifBlank { geminiConfig.model.ifBlank { "gemini-2.5-flash" } }
            logger.info { "Requesting content generation from Gemini. Model '$modelUsed'" }
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
            }.also { logger.info { "Gemini Model ${modelUsed.ifBlank { "default" }} generated content." } }
        }.getOrElse { e ->
            throw AIGenerationException(
                "Failed to generate content with Gemini",
                e
            )
        }
    }

    private fun fetchAndCleanGeminiResponse(prompt: String): String {
        val modelId = geminiConfig.quickModel.ifBlank { geminiConfig.model.ifBlank { "gemini-2.5-flash" } }
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

    override fun generateContent(prompt: String, context: ChatContext): AIResponse {
        val modelId = geminiConfig.quickModel.ifBlank { geminiConfig.model.ifBlank { "gemini-2.5-flash" } }
        logger.info { "Requesting chat generation from Gemini. Model '$modelId' with conversationId='${context.conversationId ?: "<none>"}'" }
        val MAX_TURNS = 8
        val MAX_CHARS = 4000
        try {
            val conversationPrompt = buildString {
                append("You are a helpful assistant. Use prior context when answering.\n\n")
                context.history.takeLast(MAX_TURNS).forEach { msg ->
                    val roleLabel = if (msg.role == Role.USER) "User" else "Assistant"
                    append("$roleLabel: ")
                    append(msg.text.take(MAX_CHARS))
                    append("\n\n")
                }
                append("User: ")
                append(prompt.take(MAX_CHARS))
                append("\nAssistant:")
            }

            val response = client.models.generateContent(
                modelId,
                conversationPrompt,
                null
            )

            val content = response?.text()?.cleanJsonResponse()
                ?: throw AIGenerationException("No response received from Gemini")

            return AIResponse(
                content = content,
                modelUsed = modelId
            )
        } catch (e: Exception) {
            val errorMessage = "Failed to generate content with Gemini"
            logger.error(e) { errorMessage }
            if (e is AIGenerationException) throw e
            throw AIGenerationException(errorMessage, e)
        }
    }

    private fun String.cleanJsonResponse(): String =
        replace(
            Regex("```(json)?"),
            ""
        ).trim()
}
