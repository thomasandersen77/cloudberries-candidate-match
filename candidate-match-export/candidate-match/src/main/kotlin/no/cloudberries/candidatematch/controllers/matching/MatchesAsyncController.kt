package no.cloudberries.candidatematch.controllers.matching

import mu.KotlinLogging
import no.cloudberries.candidatematch.matches.repository.ProjectMatchResultRepository
import no.cloudberries.candidatematch.matches.service.ProjectMatchingService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime

@RestController
@RequestMapping("/matches")
class MatchesAsyncController(
    private val matchingService: ProjectMatchingService,
    private val projectMatchResultRepository: ProjectMatchResultRepository,
) {
    private val logger = KotlinLogging.logger { }

    data class MatchItemDto(
        val requestId: Long,
        val consultantId: Long,
        val name: String,
        val score: Int,
        val reasons: List<String> = emptyList(),
        val profileUrl: String? = null,
        val cvQualityPercent: Int? = null,
    )

    data class MatchStatusDto(
        val status: String, // PENDING|RUNNING|COMPLETED|FAILED
        val lastUpdated: OffsetDateTime? = null,
        val error: String? = null,
    )

    @GetMapping("/{requestId}")
    @Timed
    fun getTopMatchesFlat(
        @PathVariable requestId: Long,
        @RequestParam(name = "limit", defaultValue = "10") limit: Int,
    ): ResponseEntity<List<MatchItemDto>> {
        val resp = matchingService.getMatchesForProject(requestId) ?: return ResponseEntity.ok(emptyList())
        val items = resp.matches
            .sortedByDescending { it.matchScore }
            .take(limit)
            .map { m ->
                MatchItemDto(
                    requestId = requestId,
                    consultantId = m.consultantId,
                    name = m.consultantName,
                    score = (m.matchScore * java.math.BigDecimal(100)).toInt(),
                    reasons = m.matchExplanation?.lines()?.filter { it.isNotBlank() } ?: emptyList(),
                    profileUrl = "/consultants/${m.userId}",
                    cvQualityPercent = null,
                )
            }
        return ResponseEntity.ok(items)
    }

    @GetMapping("/status/{requestId}")
    @Timed
    fun getMatchStatus(@PathVariable requestId: Long): ResponseEntity<MatchStatusDto> {
        val mostRecent = projectMatchResultRepository.findMostRecentByProjectRequestId(requestId)
        val status = if (mostRecent != null) "COMPLETED" else "PENDING"
        return ResponseEntity.ok(MatchStatusDto(status = status, lastUpdated = mostRecent?.updatedAt))
    }

    @PostMapping("/recalculate/{requestId}")
    @Timed
    fun recalculate(@PathVariable requestId: Long): ResponseEntity<Map<String, Any>> {
        logger.info { "Recalculate request for $requestId" }
        matchingService.triggerAsyncMatching(requestId, forceRecompute = true)
        val body: Map<String, Any> = mapOf(
            "requestId" to requestId,
            "statusUrl" to "/matches/status/$requestId",
            "matchesUrl" to "/matches/$requestId?limit=10"
        )
        return ResponseEntity.accepted().body(body)
    }
}