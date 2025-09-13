package no.cloudberries.candidatematch.infrastructure.entities

import jakarta.persistence.*
import no.cloudberries.candidatematch.domain.CustomerId
import no.cloudberries.candidatematch.domain.ProjectRequest
import no.cloudberries.candidatematch.domain.ProjectRequestId
import no.cloudberries.candidatematch.domain.candidate.Skill
import java.time.LocalDateTime

@Entity
@Table(name = "project_request")
data class ProjectRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "customer_id", columnDefinition = "bigint")
    val customerId: Long? = null,
    val customerName: String,
    @ElementCollection
    @CollectionTable(
        name = "project_request_required_skills",
        joinColumns = [JoinColumn(name = "project_request_id")]
    )
    @Column(name = "skill")
    @Enumerated(EnumType.STRING) // Add this line to store enum as a string
    val requiredSkills: List<Skill> = listOf(),
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    // Endret til LocalDateTime for å matche timestamp
    val responseDeadline: LocalDateTime,
    @OneToMany(mappedBy = "projectRequest", targetEntity = AISuggestionEntity::class)
    var aiSuggestionEntities: List<AISuggestionEntity> = emptyList(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RequestStatus = RequestStatus.OPEN,

    val requestDescription: String,
    val responsibleSalespersonEmail: String,
)

enum class RequestStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED
}

// Extension function to convert Entity to DTO
fun ProjectRequestEntity.toProjectRequest(): ProjectRequest {
    return ProjectRequest(
        id = ProjectRequestId(this.id),
        customerId = CustomerId(this.customerId),
        customerName = this.customerName,
        requiredSkills = this.requiredSkills,
        startDate = this.startDate,
        endDate = this.endDate,
        responseDeadline = this.responseDeadline,
        aISuggestions = this.aiSuggestionEntities.map { it.toDomain(it) },
        status = this.status,
        requestDescription = this.requestDescription,
        responsibleSalespersonEmail = this.responsibleSalespersonEmail,
    )
}
