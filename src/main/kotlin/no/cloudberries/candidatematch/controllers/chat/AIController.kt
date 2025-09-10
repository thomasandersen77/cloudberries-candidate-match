package no.cloudberries.candidatematch.controllers.chat

import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chatbot")
class AIController(
    private val aiAnalysisService: AIAnalysisService
) {
    @PostMapping("/analyze")
    fun analyzeContent(
        @RequestBody request: AIAnalysisRequest,
    ): AIResponse {
        return aiAnalysisService.analyzeContent(
            request.content,
            AIProvider.GEMINI
        )
    }
}

data class AIAnalysisRequest(
    val content: String
)