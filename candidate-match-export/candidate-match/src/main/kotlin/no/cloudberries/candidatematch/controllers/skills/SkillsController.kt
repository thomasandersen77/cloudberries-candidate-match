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

    // --- New lightweight endpoints used by the frontend ---
    data class SkillSummaryDto(val name: String, val consultantCount: Int)
    data class PageSkillSummaryDto(
        val content: List<SkillSummaryDto>,
        val number: Int,
        val size: Int,
        val totalElements: Int,
        val totalPages: Int,
        val first: Boolean,
        val last: Boolean,
        val sort: Map<String, Any?> = emptyMap(),
        val pageable: Map<String, Any?> = emptyMap()
    )

    @GetMapping("/names")
    fun listSkillNames(
        @RequestParam(required = false) prefix: String?,
        @RequestParam(required = false, defaultValue = "100") limit: Int,
    ): ResponseEntity<List<String>> {
        val names = skillsService.listSkills(null)
            .map { it.name }
            .distinct()
            .sortedBy { it.uppercase() }
            .let { list -> if (!prefix.isNullOrBlank()) list.filter { it.startsWith(prefix, ignoreCase = true) } else list }
            .take(limit.coerceAtLeast(1))
        return ResponseEntity.ok(names)
    }

    @GetMapping("/summary")
    fun listSkillSummary(
        @RequestParam(required = false, name = "q") q: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "25") size: Int,
        @RequestParam(required = false, defaultValue = "consultantCount,desc") sort: String,
    ): ResponseEntity<PageSkillSummaryDto> {
        val all = skillsService.listSkills(null)
            .map { SkillSummaryDto(name = it.name, consultantCount = it.konsulenter.size) }
            .let { list -> if (!q.isNullOrBlank()) list.filter { it.name.contains(q, ignoreCase = true) } else list }
            .let { list ->
                val parts = sort.split(",", limit = 2)
                val field = parts.getOrNull(0)
                val dir = parts.getOrNull(1)?.lowercase()
                when (field) {
                    "name" -> if (dir == "asc") list.sortedBy { it.name.lowercase() } else list.sortedByDescending { it.name.lowercase() }
                    "consultantCount" -> if (dir == "asc") list.sortedBy { it.consultantCount } else list.sortedByDescending { it.consultantCount }
                    else -> list.sortedByDescending { it.consultantCount }
                }
            }

        val total = all.size
        val safeSize = if (size <= 0) 25 else size
        val from = (page * safeSize).coerceAtMost(total)
        val to = (from + safeSize).coerceAtMost(total)
        val pageContent = all.subList(from, to)
        val totalPages = if (safeSize <= 0) 1 else (total + safeSize - 1) / safeSize

        return ResponseEntity.ok(
            PageSkillSummaryDto(
                content = pageContent,
                number = page,
                size = safeSize,
                totalElements = total,
                totalPages = totalPages,
                first = page == 0,
                last = page >= totalPages - 1
            )
        )
    }
}
