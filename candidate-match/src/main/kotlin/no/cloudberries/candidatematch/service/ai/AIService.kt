package no.cloudberries.candidatematch.service.ai

import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.domain.CandidateMatchResponse

interface AIService {

    //fun suggestResumeImprovements(resume: String, projectRequest: ProjectRequest): AIResponse
    fun matchCandidate(
        aiProvider: AIProvider,
        cv: String,
        request: String,
        consultantName: String
    ): CandidateMatchResponse
}