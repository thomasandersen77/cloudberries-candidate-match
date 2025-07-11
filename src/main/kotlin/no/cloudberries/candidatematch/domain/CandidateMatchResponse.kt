package no.cloudberries.candidatematch.domain


data class CandidateMatchResponse(
    val totalScore: String,
    val summary: String,
    val matchTimeSeconds: Int = 0,
    val requirements: List<Requirement> = listOf()
) : AIResponse

data class Requirement(
    val name: String,
    val comment: String,
    val score: String
)