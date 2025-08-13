package no.cloudberries.candidatematch.domain

import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.entities.ProjectRequestEntity
import no.cloudberries.candidatematch.entities.RequestStatus
import no.cloudberries.candidatematch.repositories.fromDomain
import java.time.LocalDate
import java.time.LocalDateTime


data class ProjectRequest(
    val id: ProjectRequestId? = null,
    val customerId: CustomerId? = null,
    val customerName: String,
    val requiredSkills: List<Skill>,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val responseDeadline: LocalDateTime,
    val aISuggestions: List<AISuggestion> = emptyList(),
    var status: RequestStatus = RequestStatus.OPEN,
    val requestDescription: String,
    val responsibleSalespersonEmail: String
) {
    init {
        require(startDate.isBefore(endDate)) { "Startdato må være før sluttdato" }
    }
}

data class ProjectRequestId(val value: Long? = null)
data class CustomerId(val value: Long? = null)

// Extension function to convert DTO to Entity
fun ProjectRequest.toEntity(): ProjectRequestEntity = ProjectRequestEntity(
    id = id?.value,
    customerId = customerId?.value,
    customerName = customerName,
    requiredSkills = requiredSkills,
    startDate = startDate,
    endDate = endDate,
    responseDeadline = responseDeadline,
    aiSuggestionEntities = aISuggestions.map { it.fromDomain(it) },
    requestDescription = requestDescription,
    responsibleSalespersonEmail = responsibleSalespersonEmail,
    status = status,
)
