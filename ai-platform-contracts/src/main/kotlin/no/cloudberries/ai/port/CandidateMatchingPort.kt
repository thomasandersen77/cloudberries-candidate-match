package no.cloudberries.ai.port

import no.cloudberries.ai.domain.CandidateMatchResponse
import no.cloudberries.ai.domain.AIProvider

interface CandidateMatchingPort {
    fun matchCandidate(
        cv: String,
        request: String,
        consultantName: String,
        provider: AIProvider? = null
    ): CandidateMatchResponse
}
