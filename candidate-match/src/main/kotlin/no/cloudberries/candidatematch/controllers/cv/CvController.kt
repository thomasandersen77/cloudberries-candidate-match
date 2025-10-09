package no.cloudberries.candidatematch.controllers.cv

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.cloudberries.candidatematch.service.consultants.ConsultantCvService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cv")
class CvController(
    private val consultantCvService: ConsultantCvService,
) {
    private val logger = KotlinLogging.logger { }

    @GetMapping("/{userId}")
    @no.cloudberries.candidatematch.utils.Timed
    fun getCv(@PathVariable userId: String): JsonNode {
        logger.info { "GET /api/cv/$userId" }
        return consultantCvService.getCvForUser(userId)
    }
}
