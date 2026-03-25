package no.cloudberries.candidatematch.service.matching

import mu.KotlinLogging
import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.domain.CandidateMatchResponse
import no.cloudberries.ai.port.CandidateMatchingPort
import no.cloudberries.candidatematch.domain.candidate.ConsultantMatchedEvent
import no.cloudberries.candidatematch.domain.event.DomainEventPublisher
import no.cloudberries.candidatematch.service.ai.AIService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CandidateMatchingService(
    private val candidateMatchingPort: CandidateMatchingPort,
    private val domainEventPublisher: DomainEventPublisher
) : AIService {
    private val logger = KotlinLogging.logger {}

    @Timed
    override fun matchCandidate(
        aiProvider: AIProvider,
        cv: String,
        request: String,
        consultantName: String
    ): CandidateMatchResponse {
        logger.debug { "Matching candidate $consultantName using ${aiProvider.name}" }
        val matchResponse = candidateMatchingPort.matchCandidate(
            cv = cv,
            request = request,
            consultantName = consultantName,
            provider = aiProvider
        )

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