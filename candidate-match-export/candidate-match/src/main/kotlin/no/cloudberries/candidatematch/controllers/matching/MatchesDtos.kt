package no.cloudberries.candidatematch.controllers.matching

import java.time.LocalDateTime

enum class CoverageStatus {
    GREEN, YELLOW, RED, NEUTRAL
}

data class MatchesListItemDto(
    val id: Long,
    val title: String?,
    val customerName: String?,
    val date: LocalDateTime?,
    val deadlineDate: LocalDateTime?,
    val hitCount: Long,
    val coverageStatus: CoverageStatus,
    val coverageLabel: String,
)

data class PagedMatchesListDto(
    val content: List<MatchesListItemDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

data class MatchConsultantDto(
    val userId: String,
    val name: String,
    val cvId: String,
    val relevanceScore: Double,
    val justification: String?,
)