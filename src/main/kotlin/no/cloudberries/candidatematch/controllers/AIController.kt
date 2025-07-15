package no.cloudberries.candidatematch.controllers

import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.service.AIAnalysisService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/ai")
class AIController(
    private val aiAnalysisService: AIAnalysisService
) {
    @PostMapping("/analyze")
    fun analyzeContent(
        @RequestBody request: AIAnalysisRequest,
        @RequestParam provider: AIProvider = AIProvider.GEMINI // Default provider
    ): AIResponse {
        return aiAnalysisService.analyzeContent(
            request.content,
            provider
        )
    }
}

data class AIAnalysisRequest(
    val content: String
)
