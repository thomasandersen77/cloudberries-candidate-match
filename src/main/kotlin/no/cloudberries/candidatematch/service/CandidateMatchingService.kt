package no.cloudberries.candidatematch.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.candidate.ConsultantMatchedEvent
import no.cloudberries.candidatematch.domain.event.DomainEventPublisher
import no.cloudberries.candidatematch.integration.AiProvider
import no.cloudberries.candidatematch.integration.gemini.GeminiHttpClient
import no.cloudberries.candidatematch.integration.openai.OpenAIHttpClient
import no.cloudberries.candidatematch.templates.MatchParams
import no.cloudberries.candidatematch.templates.MatchPromptTemplate
import no.cloudberries.candidatematch.templates.renderTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CandidateMatchingService(
    val openAIHttpClient: OpenAIHttpClient,
    val geminiHttpClient: GeminiHttpClient,
    val domainEventPublisher: DomainEventPublisher
) : AIService {
    private val logger = KotlinLogging.logger {}
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

        logger.debug { "Generated prompt for AI analysis" }

        val response = when (aiProvider) {
            AiProvider.GEMINI -> {
                logger.debug { "Using Gemini for analysis" }
                geminiHttpClient.analyze(prompt = prompt)
            }

            AiProvider.OPENAI -> {
                logger.debug { "Using OpenAI for analysis" }
                openAIHttpClient.analyze(prompt = prompt)
            }

        }

        val matchResponse = mapper.readValue<CandidateMatchResponse>(content = response)
        logger.info { "Successfully matched candidate $consultantName with score: ${matchResponse.totalScore}" }

        // Publish the ConsultantMatchedEvent
        domainEventPublisher.publish(
            ConsultantMatchedEvent(
                consultantName = consultantName,
                matchScore = matchResponse.totalScore,
                matchSummary = matchResponse.summary,
                occurredOn = Instant.now()
            )
        )

        return matchResponse
    }
}