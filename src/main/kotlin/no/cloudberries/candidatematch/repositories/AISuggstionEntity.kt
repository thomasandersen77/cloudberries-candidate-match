package no.cloudberries.candidatematch.repositories

import jakarta.persistence.*
import no.cloudberries.candidatematch.domain.AISuggestion
import no.cloudberries.candidatematch.domain.toEntity
import no.cloudberries.candidatematch.entities.ProjectRequestEntity
import no.cloudberries.candidatematch.entities.toProjectRequest

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

fun AISuggestionEntity.toDomain(entity: AISuggestionEntity): AISuggestion {
    return AISuggestion(
        id = entity.id,
        consultantName = entity.consultantName,
        userId = entity.userId,
        cvId = entity.cvId,
        matchScore = entity.matchScore,
        justification = entity.justification,
        projectRequest = entity.projectRequest?.toProjectRequest()
    )


}

