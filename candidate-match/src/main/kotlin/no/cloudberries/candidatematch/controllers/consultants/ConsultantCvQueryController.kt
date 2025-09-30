package no.cloudberries.candidatematch.controllers.consultants

import mu.KotlinLogging
import no.cloudberries.candidatematch.service.consultants.ConsultantWithCvService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// Simple page wrapper to ensure root-level pagination metadata in JSON
data class PageResponse<T>(
    val content: List<T>,
    val number: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@RestController
@RequestMapping("/api/consultants")
class ConsultantCvQueryController(
    private val consultantWithCvService: ConsultantWithCvService,
) {

    private val logger = KotlinLogging.logger { }

    @GetMapping("/with-cv")
    @no.cloudberries.candidatematch.utils.Timed
    fun getAllWithCv(
        @RequestParam(name = "onlyActiveCv", required = false, defaultValue = "false") onlyActiveCv: Boolean,
    ): List<ConsultantWithCvDto> {
        logger.info { "GET /api/consultants/with-cv onlyActiveCv=$onlyActiveCv" }
        return consultantWithCvService.getAllConsultantsWithCvs(onlyActiveCv)
    }

    @GetMapping("/with-cv/paged")
    @no.cloudberries.candidatematch.utils.Timed
    fun getAllWithCvPaged(
        @RequestParam(name = "onlyActiveCv", required = false, defaultValue = "false") onlyActiveCv: Boolean,
        @PageableDefault(size = 10) pageable: Pageable,
    ): PageResponse<ConsultantWithCvDto> {
        logger.info { "GET /api/consultants/with-cv/paged onlyActiveCv=$onlyActiveCv page=${pageable.pageNumber} size=${pageable.pageSize}" }
        val page = consultantWithCvService.getConsultantsWithCvsPaged(pageable, onlyActiveCv)
        return PageResponse(
            content = page.content,
            number = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
