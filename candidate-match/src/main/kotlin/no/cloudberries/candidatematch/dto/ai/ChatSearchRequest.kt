package no.cloudberries.candidatematch.dto.ai

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Request for intelligent consultant chat search
 */
@Schema(description = "Request for AI-powered consultant search")
data class ChatSearchRequest(
    @Schema(
        description = "Optional conversation ID to maintain context",
        example = "conv-123"
    )
    val conversationId: String? = null,

    @Schema(
        description = "Optional consultant userId to target the query (used to bias or provide RAG context)",
        example = "thomas.andersen"
    )
    val consultantId: String? = null,

    @Schema(
        description = "Optional CV/resume id for the selected consultant; used to include CV JSON as context to the AI",
        example = "default"
    )
    val cvId: String? = null,

    @Schema(
        description = "Natural language search text",
        example = "Find consultants who know Kotlin and Spring",
        required = true
    )
    val text: String,

    @Schema(
        description = "Force a specific search mode",
        example = "STRUCTURED",
        allowableValues = ["STRUCTURED", "SEMANTIC", "HYBRID", "RAG"]
    )
    val forceMode: SearchMode? = null,

    @Schema(
        description = "Maximum number of results to return",
        example = "10",
        minimum = "1",
        maximum = "50"
    )
    val topK: Int = 10
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (text.isBlank()) {
            errors.add("Search text cannot be blank")
        }

        if (topK !in 1..50) {
            errors.add("topK must be between 1 and 50")
        }

        return errors
    }
}

enum class SearchMode {
    STRUCTURED, SEMANTIC, HYBRID, RAG
}