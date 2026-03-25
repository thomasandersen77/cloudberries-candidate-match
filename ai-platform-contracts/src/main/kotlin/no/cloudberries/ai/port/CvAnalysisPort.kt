package no.cloudberries.ai.port

import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.domain.CandidateMatchResponse

interface CvAnalysisPort {
    fun analyzeCv(
        cvJson: String,
        consultantName: String,
        provider: AIProvider? = null
    ): CandidateMatchResponse
}
