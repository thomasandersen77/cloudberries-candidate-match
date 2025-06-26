package no.cloudberries.candidatematch.service

import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.integration.AiProvider

interface AIService {

    //fun suggestResumeImprovements(resume: String, projectRequest: ProjectRequest): AIResponse
    fun matchCandidate(
        aiProvider: AiProvider,
        cv: String,
        request: String,
        consultantName: String
    ): CandidateMatchResponse
}