package no.cloudberries.ai.infrastructure.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.cloudberries.ai.port.AiContentGenerationPort
import no.cloudberries.ai.port.AnalyzedProjectRequest
import no.cloudberries.ai.port.ProjectRequestAnalysisPort
import no.cloudberries.ai.templates.AnalyzeCustomerRequestPromptTemplate
import no.cloudberries.ai.templates.ProjectRequestParams
import no.cloudberries.ai.templates.renderProjectRequestTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ProjectRequestAnalysisAdapter(
    private val aiContentGenerationPort: AiContentGenerationPort
) : ProjectRequestAnalysisPort {

    private val logger = KotlinLogging.logger {}
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    override fun analyzeProjectRequest(text: String, originalFilename: String?): AnalyzedProjectRequest {
        val prompt = renderProjectRequestTemplate(
            AnalyzeCustomerRequestPromptTemplate.template,
            ProjectRequestParams(requestText = text)
        )

        val response = aiContentGenerationPort.generateContent(prompt)
        return try {
            parseResponse(response.content)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse AI response for project request analysis: ${response.content}" }
            throw RuntimeException("Failed to analyze project request", e)
        }
    }

    private fun parseResponse(content: String): AnalyzedProjectRequest {
        val json: Map<String, Any> = objectMapper.readValue(content)

        val customerName = json["Customer Name"] as? String
        val summary = json["Summary"] as? String
        val deadlineStr = json["Deadline Date"] as? String
        @Suppress("UNCHECKED_CAST")
        val requirementsMap = json["Requirements"] as? Map<String, List<String>>

        val mustRequirements = requirementsMap?.get("MUST") ?: emptyList()
        val shouldRequirements = requirementsMap?.get("SHOULD") ?: emptyList()

        val deadlineDate = deadlineStr?.let {
            try {
                LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay()
            } catch (e: Exception) {
                null
            }
        }

        return AnalyzedProjectRequest(
            customerName = customerName,
            summary = summary,
            mustRequirements = mustRequirements,
            shouldRequirements = shouldRequirements,
            uploadedAt = LocalDateTime.now(),
            deadlineDate = deadlineDate
        )
    }
}
