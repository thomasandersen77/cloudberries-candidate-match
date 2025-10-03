package no.cloudberries.candidatematch.domain.ai

/**
 * Simple chat context types for conversation-aware model generation.
 */

data class AIChatMessage(
    val role: Role,
    val text: String
)

enum class Role {
    USER,
    ASSISTANT
}

data class ChatContext(
    val conversationId: String?,
    val history: List<AIChatMessage>,
    val systemInstruction: String? = null
)
