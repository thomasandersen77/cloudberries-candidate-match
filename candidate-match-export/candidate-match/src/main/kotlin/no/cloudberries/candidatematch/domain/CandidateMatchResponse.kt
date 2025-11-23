package no.cloudberries.candidatematch.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CandidateMatchResponse(
    val consultantName: String? = null,
    val totalScore: String,
    val summary: String,
    val matchTimeSeconds: Int = 0,
    val requirements: List<Requirement> = listOf(),
    val cvImprovements: List<String> = listOf()
) : AIResponse

@JsonIgnoreProperties(ignoreUnknown = true)
data class Requirement(
    val name: String,
    val isMustHave: Boolean = false,
    val score: String,
    val justification: String? = null,
    val proposalText: String? = null,
    val yearsOfExperience: String? = null,
    // Keep backwards compatibility with old 'comment' field
    val comment: String? = null
)
