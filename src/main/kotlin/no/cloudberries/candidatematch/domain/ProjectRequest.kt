package no.cloudberries.candidatematch.domain

import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.repositories.ProjectRequestEntity
import no.cloudberries.candidatematch.repositories.fromDomain
import java.time.LocalDate


data class ProjectRequest(
    val id: ProjectRequestId? = null,
    val customerId: CustomerId? = null,
    val customerName: String,
    val requiredSkills: List<Skill>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val responseDeadline: LocalDate,
    var aISuggestions: List<AISuggestion> = emptyList()
)

data class ProjectRequestId(val value: Long? = null)
data class CustomerId(val value: Long? = null)

// Extension function to convert DTO to Entity
fun ProjectRequest.toEntity(): ProjectRequestEntity {
    return ProjectRequestEntity(
        id = this.id?.value,
        customerId = this.customerId?.value,
        customerName = customerName,
        requiredSkills = this.requiredSkills,
        startDate = this.startDate,
        endDate = this.endDate,
        responseDeadline = this.responseDeadline,
        aiSuggestionEntities = this.aISuggestions.map { it.fromDomain(it)  }
    )
}