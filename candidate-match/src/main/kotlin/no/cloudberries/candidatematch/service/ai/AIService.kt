package no.cloudberries.candidatematch.service.ai

import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.ai.AIProvider

interface AIService {

    //fun suggestResumeImprovements(resume: String, projectRequest: ProjectRequest): AIResponse
    fun matchCandidate(
        aiProvider: AIProvider,
        cv: String,
        request: String,
        consultantName: String
    ): CandidateMatchResponse
}