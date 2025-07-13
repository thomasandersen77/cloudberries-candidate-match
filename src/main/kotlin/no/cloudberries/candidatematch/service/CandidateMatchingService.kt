import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.candidate.ConsultantMatchedEvent
import no.cloudberries.candidatematch.domain.event.DomainEventPublisher
import no.cloudberries.candidatematch.integration.AiProvider
import no.cloudberries.candidatematch.integration.gemini.GeminiHttpClient
import no.cloudberries.candidatematch.integration.openai.OpenAIHttpClient
import no.cloudberries.candidatematch.service.AIService
import no.cloudberries.candidatematch.templates.MatchParams
import no.cloudberries.candidatematch.templates.MatchPromptTemplate
import no.cloudberries.candidatematch.templates.renderTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CandidateMatchingService(
    private val openAIHttpClient: OpenAIHttpClient,
    private val geminiHttpClient: GeminiHttpClient,
    private val domainEventPublisher: DomainEventPublisher
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

        logger.debug { LOG_PROMPT_GENERATED }
        val response = getAiResponse(
            aiProvider,
            prompt
        )
        return processAiResponse(
            response,
            consultantName
        )
    }

    private fun getAiResponse(aiProvider: AiProvider, prompt: String): String {
        return when (aiProvider) {
            AiProvider.GEMINI -> {
                logger.debug { LOG_USING_GEMINI }
                geminiHttpClient.analyze(prompt = prompt)
            }

            AiProvider.OPENAI -> {
                logger.debug { LOG_USING_OPENAI }
                openAIHttpClient.analyze(prompt = prompt)
            }
        }
    }

    private fun processAiResponse(response: String, consultantName: String): CandidateMatchResponse {
        val matchResponse = mapper.readValue<CandidateMatchResponse>(content = response)
        logger.info { "$LOG_MATCH_SUCCESS $consultantName with score: ${matchResponse.totalScore}" }

        publishMatchEvent(
            consultantName,
            matchResponse
        )
        return matchResponse
    }

    private fun publishMatchEvent(
        consultantName: String,
        matchResponse: CandidateMatchResponse
    ) {
        domainEventPublisher.publish(
            ConsultantMatchedEvent(
                consultantName = consultantName,
                matchScore = matchResponse.totalScore,
                matchSummary = matchResponse.summary,
                occurredOn = Instant.now()
            )
        )
    }

    companion object {
        private const val LOG_PROMPT_GENERATED = "Generated prompt for AI analysis"
        private const val LOG_USING_GEMINI = "Using Gemini for analysis"
        private const val LOG_USING_OPENAI = "Using OpenAI for analysis"
        private const val LOG_MATCH_SUCCESS = "Successfully matched candidate"
    }
}