package no.cloudberries.candidatematch.service.ai

import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.infrastructure.integration.ai.AIContentGeneratorFactory
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service

@Service
class AIAnalysisService(
    private val factory: AIContentGeneratorFactory
) {
    @Timed
    fun analyzeContent(content: String, aiProvider: AIProvider): AIResponse {
        val generator = factory.getGenerator(aiProvider)
        return generator.generateContent(content)
    }
}
