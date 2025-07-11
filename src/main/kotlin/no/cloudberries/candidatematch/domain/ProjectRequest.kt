package no.cloudberries.candidatematch.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.cloudberries.candidatematch.domain.candidate.Skill
import java.time.LocalDate

@Entity
data class ProjectRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    val customerName: String,
    val requiredSkills: List<Skill>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val responseDeadline: LocalDate,
    var aiSuggestions: List<AISuggestion> = emptyList()
)

data class AISuggestion(
    val consultantName: String,
    val score: Double,
    val justification: String
)