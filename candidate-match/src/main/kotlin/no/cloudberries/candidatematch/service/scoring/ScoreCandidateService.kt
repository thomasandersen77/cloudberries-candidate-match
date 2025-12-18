package no.cloudberries.candidatematch.service.scoring

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.scoring.CVEvaluation
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import no.cloudberries.candidatematch.templates.CvReviewParams
import no.cloudberries.candidatematch.templates.CvReviewPromptTemplate
import no.cloudberries.candidatematch.templates.renderCvReviewTemplate
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service
import kotlin.coroutines.Continuation

@Service
class ScoreCandidateService(
    private val aiAnalysisService: AIAnalysisService,
) {
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
        aiProvider: AIProvider = AIProvider.GEMINI,
        cv: String,
        consultantName: String
    ): CVEvaluation {
        val cvReviewTemplate = renderCvReviewTemplate(
            CvReviewPromptTemplate.template,
            CvReviewParams(
                cv_json = cv,
                consultantName = consultantName
            )
        )
        println(cvReviewTemplate)

        val response = when (aiProvider) {
            AIProvider.GEMINI -> {
                aiAnalysisService.analyzeContent(
                    cvReviewTemplate,
                    AIProvider.GEMINI
                )
            }

            AIProvider.OPENAI -> {
                aiAnalysisService.analyzeContent(
                    content = cvReviewTemplate,
                    AIProvider.OPENAI
                )
            }

            AIProvider.OLLAMA -> {
                aiAnalysisService.analyzeContent(
                    content = cvReviewTemplate,
                    AIProvider.OLLAMA
                )
            }

            AIProvider.ANTHROPIC -> {
                aiAnalysisService.analyzeContent(
                    content = cvReviewTemplate,
                    AIProvider.ANTHROPIC
                )
            }
        }
        val cvReviewResponseDto = mapper.readValue(
            response.content,
            CVEvaluation::class.java
        )
        return cvReviewResponseDto
    }
}
