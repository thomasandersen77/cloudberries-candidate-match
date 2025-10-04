package no.cloudberries.candidatematch.controllers.skills

import mu.KotlinLogging
import no.cloudberries.candidatematch.controllers.skills.dto.SkillInCompanyDto
import no.cloudberries.candidatematch.controllers.skills.dto.SkillSummaryDto
import no.cloudberries.candidatematch.dto.consultants.ConsultantSummaryDto
import no.cloudberries.candidatematch.service.skills.SkillsReadService
import no.cloudberries.candidatematch.service.skills.SkillsService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/skills")
class SkillsController(
    private val skillsService: SkillsService,
    private val skillsReadService: SkillsReadService,
) {
    private val logger = KotlinLogging.logger { }

    // Deprecated: Heavy aggregate. Keep for backward compatibility.
    @GetMapping
    @Timed
    fun listSkills(
        @RequestParam(name = "skill", required = false) skillFilters: List<String>?
    ): ResponseEntity<List<SkillInCompanyDto>> {
        val filterCount = skillFilters?.size ?: 0
        logger.info { "GET /api/skills (deprecated) filters=$filterCount" }
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

    // New: paginated skills summary
    @GetMapping("/summary")
    @Timed
    fun listSkillSummary(
        @RequestParam(name = "q", required = false) q: String?,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "25") size: Int,
        @RequestParam(name = "sort", required = false) sort: String?,
    ): org.springframework.data.domain.Page<SkillSummaryDto> {
        logger.info { "GET /api/skills/summary q='${q ?: ""}' page=$page size=$size sort='${sort ?: ""}'" }
        return skillsReadService.listSkillSummary(q, page, size, sort)
    }

    // New: consultants by skill (paged)
    @GetMapping("/{skill}/consultants")
    @Timed
    fun listConsultantsBySkill(
        @PathVariable("skill") skill: String,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "10") size: Int,
        @RequestParam(name = "sort", required = false) sort: String?,
    ): org.springframework.data.domain.Page<ConsultantSummaryDto> {
        logger.info { "GET /api/skills/$skill/consultants page=$page size=$size sort='${sort ?: ""}'" }
        return skillsReadService.listConsultantsBySkill(skill, page, size, sort)
    }

    // New: skill names for autocomplete
    @GetMapping("/names")
    @Timed
    fun listSkillNames(
        @RequestParam(name = "prefix", required = false) prefix: String?,
        @RequestParam(name = "limit", required = false, defaultValue = "100") limit: Int,
    ): ResponseEntity<List<String>> {
        logger.info { "GET /api/skills/names prefix='${prefix ?: ""}' limit=$limit" }
        val names = skillsReadService.listSkillNames(prefix, limit)
        return ResponseEntity.ok(names)
    }

    // New: top consultants by skill
    @GetMapping("/{skill}/top-consultants")
    @Timed
    fun listTopConsultantsBySkill(
        @PathVariable("skill") skill: String,
        @RequestParam(name = "limit", required = false, defaultValue = "3") limit: Int,
    ): ResponseEntity<List<ConsultantSummaryDto>> {
        logger.info { "GET /api/skills/$skill/top-consultants limit=$limit" }
        val result = skillsReadService.listTopConsultantsBySkill(skill, limit)
        return ResponseEntity.ok(result)
    }
}
