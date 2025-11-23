package no.cloudberries.candidatematch.infrastructure.entities

import jakarta.persistence.*
import no.cloudberries.candidatematch.domain.AISuggestion
import no.cloudberries.candidatematch.domain.toEntity

@Entity
data class AISuggestionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val consultantName: String,
    val userId: String,
    val cvId: String,
    val matchScore: Double,
    val justification: String,
    @ManyToOne(
        fetch = FetchType.LAZY,
        targetEntity = ProjectRequestEntity::class
    )
    val projectRequest: ProjectRequestEntity? = null
)

fun AISuggestion.fromDomain(domain: AISuggestion): AISuggestionEntity {
    return AISuggestionEntity(
        id = domain.id,
        consultantName = domain.consultantName,
        userId = domain.userId,
        cvId = domain.cvId,
        matchScore = domain.matchScore,
        justification = domain.justification,
        projectRequest = domain.projectRequest?.toEntity()
    )
}

fun AISuggestionEntity.toDomain(): AISuggestion {
    return AISuggestion(
        id = this.id,
        consultantName = this.consultantName,
        userId = this.userId,
        cvId = this.cvId,
        matchScore = this.matchScore,
        justification = this.justification,
        projectRequest = null // Break circular dependency - don't convert projectRequest here
    )
}

