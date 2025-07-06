package no.cloudberries.candidatematch.domain

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import no.cloudberries.candidatematch.domain.candidate.Skill
import org.springframework.data.annotation.Id
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