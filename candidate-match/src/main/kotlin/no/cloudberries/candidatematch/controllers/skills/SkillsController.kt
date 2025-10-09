package no.cloudberries.candidatematch.controllers.skills

import mu.KotlinLogging
import no.cloudberries.candidatematch.controllers.skills.dto.SkillInCompanyDto
import no.cloudberries.candidatematch.service.skills.SkillsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/skills")
class SkillsController(
    private val skillsService: SkillsService,
) {
    private val logger = KotlinLogging.logger { }

    @GetMapping
    @no.cloudberries.candidatematch.utils.Timed
    fun listSkills(
        @RequestParam(name = "skill", required = false) skillFilters: List<String>?
    ): ResponseEntity<List<SkillInCompanyDto>> {
        val filterCount = skillFilters?.size ?: 0
        logger.info { "List skills request received with filters=$filterCount" }
        val result = skillsService.listSkills(skillFilters)
        return ResponseEntity.ok(result.map { s ->
            SkillInCompanyDto(
                name = s.name,
                consultantCount = s.konsulenter.size,
                konsulenterMedSkill = s.konsulenter.size,
                konsulenter = s.konsulenter
            )
        })
    }
}
