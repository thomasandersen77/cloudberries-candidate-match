package no.cloudberries.ai.domain

interface AIResponse {
    val content: String
    val modelUsed: String
}

data class DefaultAIResponse(
    override val content: String,
    override val modelUsed: String
) : AIResponse
