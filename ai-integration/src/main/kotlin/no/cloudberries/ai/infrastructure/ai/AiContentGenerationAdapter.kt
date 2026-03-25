package no.cloudberries.ai.infrastructure.ai

import no.cloudberries.ai.config.AISettings
import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.domain.AIResponse
import no.cloudberries.ai.infrastructure.integration.anthropic.AnthropicHttpClient
import no.cloudberries.ai.infrastructure.integration.gemini.GeminiHttpClient
import no.cloudberries.ai.infrastructure.integration.ollama.OllamaHttpClient
import no.cloudberries.ai.infrastructure.integration.openai.OpenAIHttpClient
import no.cloudberries.ai.port.AiContentGenerationPort
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class AiContentGenerationAdapter(
    private val openAIHttpClient: OpenAIHttpClient,
    private val geminiHttpClient: GeminiHttpClient,
    private val ollamaHttpClient: OllamaHttpClient,
    private val anthropicHttpClient: AnthropicHttpClient,
    private val aiSettings: AISettings
) : AiContentGenerationPort {

    override fun generateContent(prompt: String, provider: AIProvider?): AIResponse {
        val selectedProvider = provider ?: aiSettings.provider
        return when (selectedProvider) {
            AIProvider.OPENAI -> openAIHttpClient.generateContent(prompt)
            AIProvider.GEMINI -> geminiHttpClient.generateContent(prompt)
            AIProvider.OLLAMA -> ollamaHttpClient.generateContent(prompt)
            AIProvider.ANTHROPIC -> anthropicHttpClient.generateContent(prompt)
        }
    }
}
