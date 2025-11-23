package no.cloudberries.candidatematch.controllers.skills.dto

import no.cloudberries.candidatematch.dto.consultants.ConsultantSummaryDto

/**
 * Public API DTO for company skills aggregation.
 * consultantCount is the preferred field; konsulenterMedSkill is kept for backward compatibility.
 */
data class SkillInCompanyDto(
    val name: String,
    val consultantCount: Int,
    val konsulenterMedSkill: Int, // deprecated
    val konsulenter: List<ConsultantSummaryDto>,
)