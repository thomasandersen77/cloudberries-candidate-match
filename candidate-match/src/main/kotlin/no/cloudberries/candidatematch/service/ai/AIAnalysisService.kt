package no.cloudberries.candidatematch.service.ai

import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.domain.AIResponse

interface AIAnalysisService {
    fun analyzeContent(content: String, provider: AIProvider? = null): AIResponse
}
