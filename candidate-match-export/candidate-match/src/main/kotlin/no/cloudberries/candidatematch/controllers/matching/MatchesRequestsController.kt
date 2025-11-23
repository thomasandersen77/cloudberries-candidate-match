package no.cloudberries.candidatematch.controllers.matching

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import mu.KotlinLogging
import no.cloudberries.candidatematch.service.matching.MatchesService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * MatchesRequestsController
 *
 * Exposes request listing and top-consultants endpoints under /matches for the UI.
 * Mirrors legacy MatchesController but without profile restrictions and with the correct base path.
 */
@Validated
@RestController
@RequestMapping("/matches")
class MatchesRequestsController(
    private val matchesService: MatchesService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Lists customer project requests with coverage information (paged).
     */
    @GetMapping("/requests-paged")
    fun listRequests(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "uploadedAt,desc") sort: String,
    ): ResponseEntity<PagedMatchesListDto> {
        logger.info { "GET /api/matches/requests-paged page=$page size=$size sort=$sort" }
        val dto = matchesService.listRequestsWithCoverage(page, size, sort)
        return ResponseEntity.ok(dto)
    }

    /**
     * Returns AI-enriched top consultants for a given project request.
     */
    @GetMapping("/requests/{id}/top-consultants")
    fun topConsultants(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "5") @Min(1) @Max(10) limit: Int,
    ): ResponseEntity<List<MatchConsultantDto>> {
        logger.info { "GET /api/matches/requests/$id/top-consultants limit=$limit" }
        val list = matchesService.getTopConsultantsWithAI(id, limit)
        return ResponseEntity.ok(list)
    }
    
    /**
     * Re-analyzes and re-ranks consultants for a given project request.
     * Uses Gemini 3.0 with batch evaluation for improved accuracy.
     * This endpoint runs synchronously and waits for results.
     */
    @PostMapping("/requests/{id}/re-analyze")
    fun reAnalyzeRequest(
        @PathVariable id: Long
    ): ResponseEntity<List<MatchConsultantDto>> {
        logger.info { "POST /api/matches/requests/$id/re-analyze - triggering re-analysis" }
        val list = matchesService.reAnalyzeWithGemini(id)
        logger.info { "Re-analysis completed for request $id, returned ${list.size} consultants" }
        return ResponseEntity.ok(list)
    }
}
