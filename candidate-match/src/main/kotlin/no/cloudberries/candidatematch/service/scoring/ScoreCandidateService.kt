package no.cloudberries.candidatematch.service.scoring

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.templates.CvReviewPromptTemplate
import no.cloudberries.candidatematch.domain.scoring.CVEvaluation
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service
import kotlin.coroutines.Continuation
import mu.KotlinLogging

@Service
class ScoreCandidateService(
    private val aiAnalysisService: AIAnalysisService,
) {
    private val logger = KotlinLogging.logger {}
    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        )
        .configure(
            DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
            false
        )

    @Timed
    fun performCvScoring(
        aiProvider: AIProvider? = null,
        cv: String,
        consultantName: String
    ): CVEvaluation {
        val prompt = CvReviewPromptTemplate.template
            .replace("{{consultantName}}", consultantName)
            .replace("{{cv_json}}", cv)

        val response = aiAnalysisService.analyzeContent(
            content = prompt,
            provider = aiProvider
        )

        val cleanedContent = cleanJsonResponse(response.content)
        logger.debug { "Cleaned content for parsing: $cleanedContent" }

        try {
            val cvReviewResponseDto = mapper.readValue(
                cleanedContent,
                CVEvaluation::class.java
            )
            return cvReviewResponseDto
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse CV evaluation JSON. Raw response: ${response.content}" }
            throw e
        }
    }

    private fun cleanJsonResponse(content: String): String {
        val jsonBlockRegex = "```json\\s*([\\s\\S]*?)\\s*```".toRegex()
        val match = jsonBlockRegex.find(content)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        val plainBlockRegex = "```\\s*([\\s\\S]*?)\\s*```".toRegex()
        val plainMatch = plainBlockRegex.find(content)
        if (plainMatch != null) {
            return plainMatch.groupValues[1].trim()
        }

        return content.trim()
    }
}
