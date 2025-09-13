package no.cloudberries.candidatematch.controllers.consultants

import no.cloudberries.candidatematch.service.consultants.ConsultantReadService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
}
