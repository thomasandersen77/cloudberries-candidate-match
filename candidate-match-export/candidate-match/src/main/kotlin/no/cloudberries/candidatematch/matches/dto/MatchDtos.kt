package no.cloudberries.candidatematch.matches.dto

import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * DTO representing a summary of a project request for matching operations.
 */
data class ProjectRequestSummaryDto(
    val id: Long,
    val title: String?,
    val customerName: String,
    val createdAt: OffsetDateTime
)

/**
 * DTO representing a candidate match result with score and explanation.
 */
data class MatchCandidateDto(
    val consultantId: Long,
    val consultantName: String,
    val userId: String,
    val cvId: String?,
    val matchScore: BigDecimal,
    val matchExplanation: String?,
    val createdAt: OffsetDateTime
)

/**
 * Response wrapper containing top candidate matches for a project request.
 */
data class MatchTop10Response(
    val projectRequestId: Long,
    val projectTitle: String?,
    val totalMatches: Int,
    val matches: List<MatchCandidateDto>,
    val lastUpdated: OffsetDateTime?
)

/**
 * Request to trigger matching computation for a project request.
 */
data class TriggerMatchingRequest(
    val forceRecompute: Boolean = false
)

/**
 * Response from triggering matching computation.
 */
data class TriggerMatchingResponse(
    val projectRequestId: Long,
    val status: String,
    val message: String,
    val jobId: String? = null
)