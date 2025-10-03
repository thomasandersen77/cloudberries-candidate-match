package no.cloudberries.candidatematch.controllers.chat

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.dto.ai.ChatSearchRequest
import no.cloudberries.candidatematch.dto.ai.ChatSearchResponse
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import no.cloudberries.candidatematch.service.ai.AISearchOrchestrator
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID
import kotlin.uuid.Uuid

@RestController
@RequestMapping("/api/chatbot")
@Tag(
    name = "AI Chat",
    description = "AI-powered consultant search and analysis"
)
class AIController(
    private val aiAnalysisService: AIAnalysisService,
    private val searchOrchestrator: AISearchOrchestrator
) {
    private val logger = KotlinLogging.logger { }

    @PostMapping("/analyze")
    @Timed
    @Operation(
        summary = "Analyze content with AI",
        description = "Legacy endpoint for AI content analysis"
    )
    fun analyzeContent(
        @RequestBody request: AIAnalysisRequest,
    ): AIResponse {
        logger.info { "POST /api/chatbot/analyze content.len=${request.content.length}" }
        request.conversationId?.let { conversationId ->
            logger.info { "Conversation ID: $conversationId" }
        }

        return aiAnalysisService.analyzeContent(
            request.content,
            AIProvider.GEMINI,
            request.conversationId
        )
    }

    @PostMapping("/search")
    @Timed
    @Operation(
        summary = "AI-powered consultant search",
        description = """
                Search consultants using natural language with intelligent routing to :
                "STRUCTURED, SEMANTIC, HYBRID, or RAG search modes
                """
    )
    fun searchChat(
        @RequestBody request: ChatSearchRequest
    ): ResponseEntity<ChatSearchResponse> {
        logger.info { "POST /api/chatbot/search: '${request.text}' (topK=${request.topK})" }

        // Validate request
        val validationErrors = request.validate()
        if (validationErrors.isNotEmpty()) {
            logger.warn { "Invalid search request: ${validationErrors.joinToString(", ")}" }
            return ResponseEntity.badRequest().build()
        }

        val response = searchOrchestrator.searchChat(request)
        return ResponseEntity.ok(response)
    }
}

data class AIAnalysisRequest(
    val content: String,
    @JsonProperty(value = "conversationId", required = false)
    val conversationId: String?
)