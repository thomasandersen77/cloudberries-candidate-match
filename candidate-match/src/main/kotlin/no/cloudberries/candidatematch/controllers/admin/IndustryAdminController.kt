package no.cloudberries.candidatematch.controllers.admin

import mu.KotlinLogging
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ConsultantCvRepository
import no.cloudberries.candidatematch.service.consultants.IndustryTaggingService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/industries")
class IndustryAdminController(
    private val consultantCvRepository: ConsultantCvRepository,
    private val industryTaggingService: IndustryTaggingService,
) {
    private val logger = KotlinLogging.logger { }

    data class BackfillResponse(val processed: Int)

    @PostMapping("/backfill")
    @Timed
    fun backfill(@RequestParam(name = "limit", required = false) limit: Int?): BackfillResponse {
        val all = consultantCvRepository.findAll()
        val toProcess = if (limit != null) all.take(limit) else all
        var count = 0
        toProcess.forEach { cv ->
            industryTaggingService.tagIndustriesForCv(cv.id!!)
            count++
        }
        logger.info { "Industry backfill processed $count CVs" }
        return BackfillResponse(processed = count)
    }
}
