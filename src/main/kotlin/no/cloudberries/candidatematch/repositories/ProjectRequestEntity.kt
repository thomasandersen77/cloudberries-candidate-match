package no.cloudberries.candidatematch.repositories

import jakarta.persistence.*
import no.cloudberries.candidatematch.domain.ProjectRequest
import no.cloudberries.candidatematch.domain.candidate.Skill
import java.time.LocalDate

@Entity
@Table(name = "project_request")
data class ProjectRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "customer_id", columnDefinition = "bigint")
    val customerId: Long? = null,
    val customerName: String? = null,
    @ElementCollection
    @CollectionTable(
        name = "project_request_required_skills",
        joinColumns = [JoinColumn(name = "project_request_id")]
    )
    @Column(name = "skill")
    @Enumerated(EnumType.STRING) // Add this line to store enum as a string
    val requiredSkills: List<Skill> = listOf(),
    val startDate: LocalDate,
    val endDate: LocalDate,
    val responseDeadline: LocalDate,
    @OneToMany(mappedBy = "projectRequest", targetEntity = AISuggestionEntity::class)
    var aiSuggestionEntities: List<AISuggestionEntity> = emptyList()
)

// Extension function to convert Entity to DTO
fun ProjectRequestEntity.toProjectRequest(): ProjectRequest {
    return ProjectRequest(
        id = this.id,
        customerId = this.customerId,
        customerName = this.customerName,
        requiredSkills = this.requiredSkills,
        startDate = this.startDate,
        endDate = this.endDate,
        responseDeadline = this.responseDeadline,
        aISuggestions = this.aiSuggestionEntities.map { it.toDomain(it) }
    )
}
