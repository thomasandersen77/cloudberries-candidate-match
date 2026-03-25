package no.cloudberries.ai.port

import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.domain.AIResponse

interface AiContentGenerationPort {
    fun generateContent(prompt: String, provider: AIProvider? = null): AIResponse
}
