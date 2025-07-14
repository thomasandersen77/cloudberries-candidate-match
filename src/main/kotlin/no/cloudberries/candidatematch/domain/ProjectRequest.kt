package no.cloudberries.candidatematch.domain

import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.repositories.AISuggestionEntity
import java.time.LocalDate


data class ProjectRequest(
    var id: Long? = null,
    val customerName: String,
    val requiredSkills: List<Skill>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val responseDeadline: LocalDate,
    var aiSuggestionEntities: List<AISuggestionEntity> = emptyList()
)