package no.cloudberries.candidatematch.domain.ai

data class AIResponse(
    val content: String,
    val modelUsed: String
) {
    companion object {
        fun empty() = AIResponse("", "")
    }
}
