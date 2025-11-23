package no.cloudberries.candidatematch.domain

import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.infrastructure.entities.ProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.entities.RequestStatus
import no.cloudberries.candidatematch.infrastructure.entities.fromDomain
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
        validateRequestDates()
        validateResponseDeadline()
        validateRequiredFields()
    }

    fun validateRequestDates() {
        require(startDate.isBefore(endDate)) { 
            "Startdato må være tidligere eller lik sluttdato" 
        }
    }
    
    fun validateResponseDeadline() {
        require(responseDeadline.isBefore(startDate)) {
            "Svarfristen kan ikke være etter prosjektets startdato"
        }
    }
    
    fun validateRequiredFields() {
        require(customerName.isNotBlank()) { "Kundenavn må angis" }
        require(requestDescription.isNotBlank()) { "Prosjektbeskrivelse må angis" }
        require(responsibleSalespersonEmail.isNotBlank()) { "Ansvarlig selger må angis" }
    }

    fun closeRequest() {
        require(status != RequestStatus.CLOSED) { "Forespørselen er allerede lukket" }
        status = RequestStatus.CLOSED
    }
    
    fun addAISuggestion(suggestion: AISuggestion): ProjectRequest {
        return this.copy(aISuggestions = this.aISuggestions + suggestion)
    }
    
    fun getTopSkills(count: Int = 3): List<Skill> {
        return requiredSkills.take(count)
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
