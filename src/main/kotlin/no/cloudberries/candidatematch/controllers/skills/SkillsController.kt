package no.cloudberries.candidatematch.controllers.skills

import mu.KotlinLogging
import no.cloudberries.candidatematch.controllers.consultants.ConsultantSummaryDto
import no.cloudberries.candidatematch.service.skills.SkillsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/skills")
class SkillsController(
    private val skillsService: SkillsService,
) {
    private val logger = KotlinLogging.logger { }

    data class SkillInCompanyDto(
        val name: String,
        val konsulenterMedSkill: Int,
        val konsulenter: List<ConsultantSummaryDto>,
    )

    @GetMapping
    fun listSkills(
        @RequestParam(name = "skill", required = false) skillFilters: List<String>?
    ): ResponseEntity<List<SkillInCompanyDto>> {
        logger.info { "List skills${if (!skillFilters.isNullOrEmpty()) " filter=" + skillFilters.joinToString(",") else ""}" }
        val result = skillsService.listSkills(skillFilters)
        return ResponseEntity.ok(result.map { s ->
            SkillInCompanyDto(
                name = s.name,
                konsulenterMedSkill = s.konsulenter.size,
                konsulenter = s.konsulenter
            )
        })
    }
}