package no.cloudberries.ai.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CandidateMatchResponse(
    val consultantName: String? = null,
    val totalScore: String,
    val summary: String,
    val matchTimeSeconds: Int = 0,
    val requirements: List<Requirement> = listOf(),
    val cvImprovements: List<String> = listOf()
) : AIResponse {
    override val content: String get() = summary
    override val modelUsed: String get() = "AI"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Requirement(
    val name: String,
    val isMustHave: Boolean = false,
    val score: String,
    val justification: String? = null,
    val proposalText: String? = null,
    val yearsOfExperience: String? = null,
    val comment: String? = null
)
