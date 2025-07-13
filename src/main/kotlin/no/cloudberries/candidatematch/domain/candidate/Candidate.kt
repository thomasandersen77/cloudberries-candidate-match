package no.cloudberries.candidatematch.domain.candidate

sealed interface Candidate {
    val name: String
    val userId: String
    val cvId: String
}
