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
    val aISuggestions: List<AISuggestion> = emptyList()
)

data class ProjectRequestId(val value: Long? = null)
data class CustomerId(val value: Long? = null)

// Extension function to convert DTO to Entity
fun ProjectRequest.toEntity(): ProjectRequestEntity {
    return ProjectRequestEntity(
        id = id?.value,
        customerId = customerId?.value,
        customerName = customerName,
        requiredSkills = requiredSkills,
        startDate = startDate,
        endDate = endDate,
        responseDeadline = responseDeadline,
        aiSuggestionEntities = aISuggestions.map { it.fromDomain(it)  }
    )
}