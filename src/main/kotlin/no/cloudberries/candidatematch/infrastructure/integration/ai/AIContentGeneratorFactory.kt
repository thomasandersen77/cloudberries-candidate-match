package no.cloudberries.candidatematch.infrastructure.integration.ai

import no.cloudberries.candidatematch.domain.ai.AIContentGenerator
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.infrastructure.integration.gemini.GeminiHttpClient
import no.cloudberries.candidatematch.infrastructure.integration.openai.OpenAIHttpClient
import org.springframework.stereotype.Component

@Component
class AIContentGeneratorFactory(
    private val openAIHttpClient: OpenAIHttpClient,
    private val geminiHttpClient: GeminiHttpClient
) {
    fun getGenerator(provider: AIProvider): AIContentGenerator {
        return when (provider) {
            AIProvider.OPENAI -> openAIHttpClient
            AIProvider.GEMINI -> geminiHttpClient
        }
    }
}
