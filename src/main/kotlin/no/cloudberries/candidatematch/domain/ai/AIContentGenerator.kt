package no.cloudberries.candidatematch.domain.ai

interface AIContentGenerator {
    fun generateContent(prompt: String): AIResponse
}
