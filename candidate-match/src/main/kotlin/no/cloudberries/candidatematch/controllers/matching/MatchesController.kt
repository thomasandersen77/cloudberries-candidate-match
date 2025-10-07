package no.cloudberries.candidatematch.controllers.matching

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import mu.KotlinLogging
import no.cloudberries.candidatematch.controllers.matching.PagedMatchesListDto
import no.cloudberries.candidatematch.controllers.matching.MatchConsultantDto
import no.cloudberries.candidatematch.service.matching.MatchesService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@Profile("!local")
@RestController
@RequestMapping("/api/matches")
class MatchesController(
    private val matchesService: MatchesService
) {
    private val logger = KotlinLogging.logger {}

    @GetMapping("/requests")
    fun listRequests(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "uploadedAt,desc") sort: String,
    ): ResponseEntity<PagedMatchesListDto> {
        logger.info { "GET /api/matches/requests page=$page size=$size sort=$sort" }
        val dto = matchesService.listRequestsWithCoverage(page, size, sort)
        return ResponseEntity.ok(dto)
    }

    @GetMapping("/requests/{id}/top-consultants")
    fun topConsultants(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "5") @Min(1) @Max(10) limit: Int,
    ): ResponseEntity<List<MatchConsultantDto>> {
        logger.info { "GET /api/matches/requests/$id/top-consultants limit=$limit" }
        val list = matchesService.getTopConsultantsWithAI(id, limit)
        return ResponseEntity.ok(list)
    }
}