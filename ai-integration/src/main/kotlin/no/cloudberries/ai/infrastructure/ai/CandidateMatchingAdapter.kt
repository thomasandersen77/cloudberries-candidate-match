package no.cloudberries.ai.infrastructure.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.domain.CandidateMatchResponse
import no.cloudberries.ai.port.AiContentGenerationPort
import no.cloudberries.ai.port.CandidateMatchingPort
import no.cloudberries.ai.templates.MatchParams
import no.cloudberries.ai.templates.MatchPromptTemplate
import no.cloudberries.ai.templates.renderMatchTemplate
import org.springframework.stereotype.Service

@Service
class CandidateMatchingAdapter(
    private val aiContentGenerationPort: AiContentGenerationPort
) : CandidateMatchingPort {

    private val logger = KotlinLogging.logger {}
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    override fun matchCandidate(
        cv: String,
        request: String,
        consultantName: String,
        provider: AIProvider?
    ): CandidateMatchResponse {
        val prompt = renderMatchTemplate(
            MatchPromptTemplate.template,
            MatchParams(
                cv = cv,
                request = request,
                consultantName = consultantName
            )
        )

        val response = aiContentGenerationPort.generateContent(prompt, provider)
        return try {
            parseResponse(response.content)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse AI response for candidate matching: ${response.content}" }
            throw RuntimeException("Failed to match candidate", e)
        }
    }

    private fun parseResponse(content: String): CandidateMatchResponse {
        return objectMapper.readValue(content)
    }
}
