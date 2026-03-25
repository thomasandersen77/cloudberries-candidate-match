package no.cloudberries.ai.infrastructure.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.cloudberries.ai.dto.ConfidenceScores
import no.cloudberries.ai.dto.QueryInterpretation
import no.cloudberries.ai.dto.SearchMode
import no.cloudberries.ai.dto.StructuredCriteria
import no.cloudberries.ai.port.AiContentGenerationPort
import no.cloudberries.ai.port.QueryInterpretationPort
import no.cloudberries.ai.templates.QueryInterpretationPromptTemplate
import no.cloudberries.ai.templates.QueryInterpretationParams
import org.springframework.stereotype.Service
import kotlin.math.max
import kotlin.math.min

/**
 * Prompt → AI → JSON → [QueryInterpretation] (contracts). No candidate-match dependencies.
 */
@Service
class QueryInterpretationAdapter(
    private val aiContentGenerationPort: AiContentGenerationPort,
    private val objectMapper: ObjectMapper
) : QueryInterpretationPort {

    private val logger = KotlinLogging.logger { }

    override fun interpretQuery(userText: String, forceMode: SearchMode?): QueryInterpretation {
        val prompt = QueryInterpretationPromptTemplate.render(QueryInterpretationParams(userText = userText))
        val rawContent = aiContentGenerationPort.generateContent(prompt).content
        val interpretation = parseInterpretationResponse(rawContent)
        return if (forceMode != null) {
            interpretation.copy(route = forceMode)
        } else {
            interpretation
        }
    }

    private fun parseInterpretationResponse(content: String): QueryInterpretation {
        return try {
            val cleanJson = stripMarkdownFences(content)
            val parsed = objectMapper.readValue<InterpretationJson>(cleanJson)
            parsed.toQueryInterpretation()
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse AI interpretation response: $content" }
            throw e as? QueryInterpretationException ?: QueryInterpretationException("Failed to parse AI interpretation", e)
        }
    }

    private fun stripMarkdownFences(raw: String): String {
        return raw
            .replace("```json", "")
            .replace("```", "")
            .trim()
    }
}

class QueryInterpretationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

private data class InterpretationJson(
    val route: String,
    val structured: StructuredJson?,
    val semanticText: String?,
    val consultantName: String?,
    val question: String?,
    val confidence: ConfidenceJson
) {
    fun toQueryInterpretation(): QueryInterpretation {
        return QueryInterpretation(
            route = route.toSearchMode(),
            structured = structured?.toStructuredCriteria(),
            semanticText = semanticText,
            consultantName = consultantName,
            question = question,
            confidence = confidence.toConfidenceScores()
        )
    }
}

private data class StructuredJson(
    val skillsAll: List<String>?,
    val skillsAny: List<String>?,
    val roles: List<String>?,
    val minQualityScore: Int?,
    val locations: List<String>?,
    val availability: String?,
    val publicSector: Boolean?,
    val customersAny: List<String>?,
    val industries: List<String>?
) {
    fun toStructuredCriteria(): StructuredCriteria {
        return StructuredCriteria(
            skillsAll = skillsAll ?: emptyList(),
            skillsAny = skillsAny ?: emptyList(),
            roles = roles ?: emptyList(),
            minQualityScore = minQualityScore,
            locations = locations ?: emptyList(),
            availability = availability,
            publicSector = publicSector,
            customersAny = customersAny ?: emptyList(),
            industries = industries ?: emptyList()
        )
    }
}

private data class ConfidenceJson(
    val route: Double,
    val extraction: Double
) {
    fun toConfidenceScores(): ConfidenceScores {
        return ConfidenceScores(
            route = clamp01(route),
            extraction = clamp01(extraction)
        )
    }
}

private fun clamp01(value: Double): Double = max(0.0, min(1.0, value))

private fun String.toSearchMode(): SearchMode = when (lowercase()) {
    "structured" -> SearchMode.STRUCTURED
    "semantic" -> SearchMode.SEMANTIC
    "hybrid" -> SearchMode.HYBRID
    "rag" -> SearchMode.RAG
    else -> throw QueryInterpretationException("Unknown route value: $this")
}
