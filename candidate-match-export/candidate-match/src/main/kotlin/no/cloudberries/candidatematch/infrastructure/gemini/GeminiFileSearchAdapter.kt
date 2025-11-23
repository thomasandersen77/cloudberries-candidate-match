package no.cloudberries.candidatematch.infrastructure.gemini

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import no.cloudberries.candidatematch.application.ports.CandidateSnapshot
import no.cloudberries.candidatematch.application.ports.GeminiMatchingPort
import no.cloudberries.candidatematch.application.ports.RankedCandidateDto
import no.cloudberries.candidatematch.config.GeminiConfig
import no.cloudberries.candidatematch.config.GeminiProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.time.Duration

/**
 * Infrastructure adapter for Gemini File Search integration.
 * Implements managed RAG (Retrieval-Augmented Generation) using Gemini's File API.
 * 
 * Responsibilities:
 * - Manage File Search store lifecycle
 * - Upload CV documents to Gemini
 * - Execute semantic search and ranking via generateContent
 * - Parse structured JSON responses
 */
@Component
class GeminiFileSearchAdapter(
    private val webClient: WebClient,
    private val geminiProperties: GeminiProperties,
    private val matchingProperties: no.cloudberries.candidatematch.config.MatchingProperties,
    private val objectMapper: ObjectMapper
) : GeminiMatchingPort {

    private val logger = KotlinLogging.logger {}
    
    private val storeName = geminiProperties.fileStoreName
    private val model = matchingProperties.model  // Use matching.model instead of gemini.model

    override suspend fun ensureStore(): String {
        // No-op: We don't use File Search store anymore
        // CVs are sent inline in the request
        return "inline"
    }

    override suspend fun uploadCv(fileId: String, filename: String, bytes: ByteArray): String {
        // No-op: We don't upload CVs separately anymore
        // CVs are sent inline in the rankCandidates request
        logger.debug { "CV upload not needed - using inline approach" }
        return "inline:$fileId"
    }

    override suspend fun rankCandidates(
        projectRequestId: String,
        projectDescription: String,
        candidates: List<CandidateSnapshot>,
        topN: Int
    ): List<RankedCandidateDto> {
        logger.info { "Ranking candidates for project $projectRequestId (topN=$topN, candidates=${candidates.size})" }
        
        if (candidates.isEmpty()) {
            logger.warn { "No candidates provided for ranking" }
            return emptyList()
        }

        try {
            val systemPrompt = buildSystemPrompt()
            
            // Build user prompt with CV texts embedded inline
            val userPrompt = buildUserPromptWithCvs(projectDescription, candidates, topN)
            
            val requestBody = buildGenerateContentRequest(systemPrompt, userPrompt)
            
            val response = webClient.post()
                .uri("/v1/models/$model:generateContent")  // Note: v1, not v1beta
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .awaitBody<Map<String, Any>>()
            
            val textResponse = extractTextFromResponse(response)
            val rankedResult = parseRankedJson(textResponse)
            
            logger.info { "Successfully ranked ${rankedResult.ranked.size} candidates" }
            return rankedResult.ranked.take(topN).map {
                RankedCandidateDto(
                    consultantId = it.consultantId,
                    score = it.score,
                    reasons = it.reasons
                )
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to rank candidates for project $projectRequestId" }
            return emptyList() // Graceful degradation
        }
    }

    private fun buildSystemPrompt(): String = """
        Du er en teknisk rekrutteringsassistent som skal rangere konsulenter mot kundeforespørsler.
        
        Du får:
        1. En kundeforespørsel med krav og beskrivelse
        2. Flere konsulenter med deres fullstendige CV-er
        
        Vektlegg:
        - "Må"-krav viktigere enn "bør"-krav
        - Relevant erfaring (prosjekter, teknologier, domene)
        - CV-kvalitet og fullstendighet
        - Semantisk match (ikke bare eksakte ord)
        
        Returner KUN valid JSON:
        {
          "projectRequestId": "<id>",
          "ranked": [
            {
              "consultantId": "<id>",
              "score": 0-100,
              "reasons": ["konkret grunn 1", "konkret grunn 2", "konkret grunn 3"]
            }
          ]
        }
        
        VIKTIG: Basér vurderingen på CV-innholdet nedenfor, ikke på generell kunnskap.
    """.trimIndent()

    /**
     * Builds user prompt with full CV texts embedded inline.
     * This allows Gemini to analyze CVs directly without File Search.
     */
    private fun buildUserPromptWithCvs(
        projectDescription: String,
        candidates: List<CandidateSnapshot>,
        topN: Int
    ): String {
        val cvsText = candidates.joinToString("\n\n" + "=".repeat(80) + "\n\n") { candidate ->
            buildString {
                appendLine("KONSULENT ID: ${candidate.consultantId}")
                appendLine("Navn: ${candidate.name ?: "Ikke oppgitt"}")
                appendLine("CV-kvalitet: ${candidate.cvQuality}/100")
                appendLine()
                // Include full CV text directly (stored in cvGeminiUri field)
                if (candidate.cvGeminiUri?.isNotBlank() == true) {
                    appendLine(candidate.cvGeminiUri)
                } else {
                    appendLine("FERDIGHETER:")
                    candidate.skills.forEach { skill ->
                        appendLine("- $skill")
                    }
                    appendLine("(Ingen detaljert CV tilgjengelig)")
                }
            }
        }

        return """
            KUNDEFORESPØRSEL:
            $projectDescription
            
            ${"".repeat(80)}
            
            KONSULENTER (${candidates.size} totalt):
            
            $cvsText
            
            ${"".repeat(80)}
            
            OPPGAVE:
            Ranger de ${minOf(topN, candidates.size)} best egnede konsulentene for denne kundeforespørselen.
            
            Vurder:
            1. Match mot krav (må-krav > bør-krav)
            2. Relevant erfaring fra CV
            3. Teknisk kompetanse
            4. Bransjeerfaring
            
            Returner KUN JSON som beskrevet i systemprompt.
        """.trimIndent()
    }

    private fun buildGenerateContentRequest(systemPrompt: String, userPrompt: String): Map<String, Any> {
        return mapOf(
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to listOf(
                        mapOf("text" to "$systemPrompt\n\n$userPrompt")
                    )
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to 0.2,  // Low temperature for consistent structured output
                "topP" to 0.9,
                "topK" to 40,
                "maxOutputTokens" to 2048,
                "responseMimeType" to "application/json"  // Force JSON response
            )
        )
    }

    private fun extractTextFromResponse(response: Map<String, Any>): String {
        val candidates = response["candidates"] as? List<*> ?: emptyList<Any>()
        if (candidates.isEmpty()) {
            throw GeminiParseException("No candidates in response")
        }
        
        val firstCandidate = candidates.first() as? Map<*, *> 
            ?: throw GeminiParseException("Invalid candidate structure")
        
        val content = firstCandidate["content"] as? Map<*, *>
            ?: throw GeminiParseException("No content in candidate")
        
        val parts = content["parts"] as? List<*>
            ?: throw GeminiParseException("No parts in content")
        
        val firstPart = parts.firstOrNull() as? Map<*, *>
            ?: throw GeminiParseException("No parts available")
        
        return firstPart["text"] as? String
            ?: throw GeminiParseException("No text in part")
    }

    private fun parseRankedJson(jsonText: String): RankedJsonResponse {
        try {
            // Clean up any markdown code blocks if present
            val cleanJson = jsonText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            return objectMapper.readValue<RankedJsonResponse>(cleanJson)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse ranked JSON: $jsonText" }
            throw GeminiParseException("Invalid JSON response from Gemini", e)
        }
    }

    private fun extractFileUri(uploadResponse: Map<String, Any>): String {
        val file = uploadResponse["file"] as? Map<*, *>
            ?: throw GeminiUploadException("No file in upload response")
        
        return file["uri"] as? String
            ?: throw GeminiUploadException("No URI in file response")
    }

    // DTOs for JSON parsing
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RankedJsonResponse(
        val projectRequestId: String,
        val ranked: List<RankedItem>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RankedItem(
        val consultantId: String,
        val score: Int,
        val reasons: List<String>
    )
}

/**
 * Exception thrown when Gemini API upload fails.
 */
class GeminiUploadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Exception thrown when parsing Gemini API response fails.
 */
class GeminiParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
