package no.cloudberries.candidatematch.domain.candidate

import no.cloudberries.candidatematch.domain.Identifiable

data class Candidate(
    override val id: String,
    val name: String,
    val birthYear: Int,
) : Identifiable {

}
