package no.cloudberries.candidatematch.service.ai

import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.domain.AIResponse
import no.cloudberries.ai.port.AiContentGenerationPort
import org.springframework.stereotype.Service

@Service
class AIContentAnalysisService(
    private val aiContentGenerationPort: AiContentGenerationPort
) : AIAnalysisService {

    override fun analyzeContent(content: String, provider: AIProvider?): AIResponse {
        return aiContentGenerationPort.generateContent(content, provider)
    }
}
