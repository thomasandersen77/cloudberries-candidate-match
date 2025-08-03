package no.cloudberries.candidatematch.domain

import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.repositories.ProjectRequestEntity
import no.cloudberries.candidatematch.repositories.fromDomain
import java.time.LocalDate


data class ProjectRequest(
    var id: Long? = null,
    val customerId: Long,
    val customerName: String? = null,
    val requiredSkills: List<Skill>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val responseDeadline: LocalDate,
    var aISuggestions: List<AISuggestion> = emptyList()
)

// Extension function to convert DTO to Entity
fun ProjectRequest.toEntity(): ProjectRequestEntity {
    return ProjectRequestEntity(
        id = this.id,
        customerId = this.customerId,
        customerName = customerName,
        requiredSkills = this.requiredSkills,
        startDate = this.startDate,
        endDate = this.endDate,
        responseDeadline = this.responseDeadline,
        aiSuggestionEntities = this.aISuggestions.map { it.fromDomain(it)  }
    )
}