package no.cloudberries.candidatematch.controllers.cv

import com.fasterxml.jackson.databind.JsonNode
import no.cloudberries.candidatematch.service.consultants.ConsultantCvService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cv")
class CvController(
    private val consultantCvService: ConsultantCvService,
) {
    @GetMapping("/{userId}")
    fun getCv(@PathVariable userId: String): JsonNode = consultantCvService.getCvForUser(userId)
}