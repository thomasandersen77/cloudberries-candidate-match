package no.cloudberries.candidatematch.dto.ai

import io.swagger.v3.oas.annotations.media.Schema
import no.cloudberries.candidatematch.domain.consultant.RelationalSearchCriteria
import java.time.LocalDateTime

/**
 * AI interpretation of user query to determine search route and extract criteria
 */
@Schema(description = "AI interpretation of user search query")
data class QueryInterpretation(
    @Schema(
        description = "Determined search route",
        example = "structured"
    )
    val route: SearchMode,

    @Schema(description = "Extracted structured search criteria")
    val structured: StructuredCriteria? = null,

    @Schema(
        description = "Semantic search text",
        example = "experienced fullstack developer"
    )
    val semanticText: String? = null,

    @Schema(
        description = "Detected consultant name",
        example = "Thomas Andersen"
    )
    val consultantName: String? = null,

    @Schema(
        description = "RAG question",
        example = "What is his experience with React?"
    )
    val question: String? = null,

    @Schema(description = "Confidence scores for the interpretation")
    val confidence: ConfidenceScores
)

/**
 * Structured search criteria extracted from natural language
 */
@Schema(description = "Structured search criteria")
data class StructuredCriteria(
    @Schema(
        description = "Skills that must all be present",
        example = "[\"kotlin\", \"spring\"]"
    )
    val skillsAll: List<String> = emptyList(),

    @Schema(
        description = "Skills where at least one must be present",
        example = "[\"architecture\", \"tech lead\"]"
    )
    val skillsAny: List<String> = emptyList(),

    @Schema(
        description = "Required roles or positions",
        example = "[\"senior developer\", \"tech lead\"]"
    )
    val roles: List<String> = emptyList(),

    @Schema(
        description = "Minimum quality score",
        example = "85"
    )
    val minQualityScore: Int? = null,

    @Schema(description = "Location requirements")
    val locations: List<String> = emptyList(),

    @Schema(
        description = "Availability requirements",
        example = "available"
    )
    val availability: String? = null,

    @Schema(
        description = "True if the customer/employer context should be public sector",
        example = "true"
    )
    val publicSector: Boolean? = null,

    @Schema(
        description = "Keywords for target customers/industries (e.g., ['sparebank1','bank'])"
    )
    val customersAny: List<String> = emptyList(),

    @Schema(
        description = "Industry keywords (optional; for future use)",
        example = "[\"finance\", \"public\"]"
    )
    val industries: List<String> = emptyList()
) {
    /**
     * Convert to existing RelationalSearchCriteria for compatibility
     */
    fun toRelationalSearchCriteria(): RelationalSearchCriteria {
        return RelationalSearchCriteria(
            name = null,
            skillsAll = skillsAll,
            skillsAny = skillsAny,
            minQualityScore = minQualityScore,
            onlyActiveCv = true,
            publicSector = publicSector,
            customersAny = customersAny,
            industriesAny = industries
        )
    }
}

/**
 * Confidence scores for AI interpretation
 */
@Schema(description = "AI confidence scores")
data class ConfidenceScores(
    @Schema(
        description = "Confidence in route selection (0-1)",
        example = "0.87"
    )
    val route: Double,

    @Schema(
        description = "Confidence in criteria extraction (0-1)",
        example = "0.92"
    )
    val extraction: Double
)

/**
 * Conversation turn for memory
 */
@Schema(description = "Individual conversation turn")
data class ConversationTurn(
    val id: String,
    val conversationId: String,
    val userText: String,
    val aiMode: SearchMode,
    val interpretation: QueryInterpretation,
    val timestamp: LocalDateTime = LocalDateTime.now()
)