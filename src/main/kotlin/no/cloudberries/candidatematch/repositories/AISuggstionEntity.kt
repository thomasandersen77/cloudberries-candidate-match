package no.cloudberries.candidatematch.repositories

import jakarta.persistence.*

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