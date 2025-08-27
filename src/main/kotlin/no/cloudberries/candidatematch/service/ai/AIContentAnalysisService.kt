package no.cloudberries.candidatematch.service.ai

import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.integration.ai.AIContentGeneratorFactory
import org.springframework.stereotype.Service

@Service
class AIAnalysisService(
    private val factory: AIContentGeneratorFactory
) {
    fun analyzeContent(content: String, aiProvider: AIProvider): AIResponse {
        val generator = factory.getGenerator(aiProvider)
        return generator.generateContent(content)
    }
}
