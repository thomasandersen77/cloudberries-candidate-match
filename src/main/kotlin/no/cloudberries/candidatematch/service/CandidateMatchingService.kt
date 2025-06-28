package no.cloudberries.candidatematch.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.integration.AiProvider
import no.cloudberries.candidatematch.integration.gemini.GeminiHttpClient
import no.cloudberries.candidatematch.integration.openai.OpenAIHttpClient
import no.cloudberries.candidatematch.templates.MatchParams
import no.cloudberries.candidatematch.templates.MatchPromptTemplate
import no.cloudberries.candidatematch.templates.renderTemplate
import org.springframework.stereotype.Service

@Service
class CandidateMatchingService(
    val openAIHttpClient: OpenAIHttpClient,
    val geminiHttpClient: GeminiHttpClient

) : AIService {

    private val mapper = jacksonObjectMapper()

    override fun matchCandidate(
        aiProvider: AiProvider,
        cv: String,
        request: String,
        consultantName: String
    ): CandidateMatchResponse {
        val prompt = renderTemplate(
            MatchPromptTemplate.template,
            MatchParams(
                cv = cv,
                request = request,
                consultantName = consultantName
            )
        )

        val response = when (aiProvider) {
            AiProvider.GEMINI -> geminiHttpClient.analyze(prompt = prompt)
            AiProvider.OPENAI -> openAIHttpClient.analyze(prompt = prompt)
        }

        return mapper.readValue<CandidateMatchResponse>(content = response)
    }
}