package no.cloudberries.candidatematch.service.ai

import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.ai.AIChatMessage
import no.cloudberries.candidatematch.domain.ai.ChatContext
import no.cloudberries.candidatematch.domain.ai.Role
import no.cloudberries.candidatematch.infrastructure.integration.ai.AIContentGeneratorFactory
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service

@Service
class AIAnalysisService(
    private val factory: AIContentGeneratorFactory,
    private val conversationService: ConversationService
) {
    @Timed
    fun analyzeContent(content: String, aiProvider: AIProvider): AIResponse {
        return analyzeContent(content, aiProvider, null)
    }

    @Timed
    fun analyzeContent(content: String, aiProvider: AIProvider, conversationId: String?): AIResponse {
        val generator = factory.getGenerator(aiProvider)
        if (conversationId.isNullOrBlank()) {
            return generator.generateContent(content)
        }

        val turns = conversationService.getConversationHistory(conversationId)
        val history = mutableListOf<AIChatMessage>()
        turns.forEach { t ->
            history.add(AIChatMessage(Role.USER, t.question))
            history.add(AIChatMessage(Role.ASSISTANT, t.answer))
        }

        val ctx = ChatContext(
            conversationId = conversationId,
            history = history
        )

        val response = generator.generateContent(content, ctx)
        // Persist combined turn (question + answer)
        conversationService.addToConversation(conversationId, content, response.content)
        return response
    }
}
