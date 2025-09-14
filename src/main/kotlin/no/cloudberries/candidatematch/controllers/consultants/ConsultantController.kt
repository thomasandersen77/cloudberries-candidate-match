package no.cloudberries.candidatematch.controllers.consultants

import no.cloudberries.candidatematch.service.consultants.ConsultantReadService
import no.cloudberries.candidatematch.service.consultants.SyncConsultantService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// Only read operations from Flowcase via service. No create/update.

data class ConsultantSummaryDto(
    val userId: String,
    val name: String,
    val email: String,
    val bornYear: Int,
    val defaultCvId: String,
)

@RestController
@RequestMapping("/api/consultants")
class ConsultantController(
    private val consultantReadService: ConsultantReadService,
    private val syncConsultantService: SyncConsultantService,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) name: String?,
        @PageableDefault(size = 10) pageable: Pageable
    ): Page<ConsultantSummaryDto> {
        return consultantReadService.listConsultants(
            name,
            pageable
        )
    }

    @PostMapping("/sync/run")
    fun runSync(
        @RequestParam(
            name = "batchSize",
            defaultValue = "100"
        ) batchSize: Int,
    ): ResponseEntity<Map<String, Any>> {
        val size = batchSize.coerceIn(
            1,
            1000
        )
        val result = syncConsultantService.syncAll(size)
        return ResponseEntity.ok(
            mapOf(
                "processed" to result.processed,
                "skipped" to result.skipped,
                "errors" to result.errors
            )
        )
    }
}
