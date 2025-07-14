package no.cloudberries.candidatematch.repositories

import jakarta.persistence.*
import no.cloudberries.candidatematch.domain.candidate.Skill
import java.time.LocalDate

@Entity
@Table(name = "project_request")
data class ProjectRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    val customerName: String,
    val requiredSkills: List<Skill>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val responseDeadline: LocalDate,
    @OneToMany(mappedBy = "projectRequest", targetEntity = AISuggestionEntity::class)
    var aiSuggestionEntities: List<AISuggestionEntity> = emptyList()
)
