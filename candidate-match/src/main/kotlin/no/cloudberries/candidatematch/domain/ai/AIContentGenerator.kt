package no.cloudberries.candidatematch.domain.ai

interface AIContentGenerator {
    fun generateContent(prompt: String): AIResponse

    // Default no-op implementation for chat-aware generation. Implementers can override.
    fun generateContent(prompt: String, context: ChatContext): AIResponse = generateContent(prompt)
}
