package no.cloudberries.candidatematch.service.skills

import no.cloudberries.candidatematch.controllers.skills.dto.SkillSummaryDto
import no.cloudberries.candidatematch.dto.consultants.ConsultantSummaryDto
import org.springframework.data.domain.Page

interface SkillsReadService {
    fun listSkillSummary(q: String?, page: Int, size: Int, sort: String?): Page<SkillSummaryDto>
    fun listConsultantsBySkill(skill: String, page: Int, size: Int, sort: String?): Page<ConsultantSummaryDto>
    fun listSkillNames(prefix: String?, limit: Int): List<String>
    fun listTopConsultantsBySkill(skill: String, limit: Int): List<ConsultantSummaryDto>
}
