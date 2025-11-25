package no.cloudberries.candidatematch.infrastructure.gemini

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import no.cloudberries.candidatematch.config.GeminiProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.time.Duration

/**
 * Adapter for Gemini Files API (v1beta) using Long Context approach.
 * 
 * Implements resumable file upload and generateContent with file_data references.
 * This approach is more efficient than sending CVs inline for repeated use cases.
 * 
 * API Documentation: https://ai.google.dev/api/files
 * Long Context Guide: https://ai.google.dev/gemini-api/docs/long-context
 */
@Component
class GeminiFilesApiAdapter(
    private val geminiWebClient: WebClient,
    private val geminiProperties: GeminiProperties,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger {}
    
    private val uploadTimeout = Duration.ofSeconds(60)
    private val generateTimeout = Duration.ofMinutes(5)

    /**
     * Uploads CV content as Markdown using resumable upload protocol.
     * 
     * Two-step process:
     * 1. POST to /upload/v1beta/files with metadata → get upload URL
     * 2. POST bytes to upload URL → get file URI
     * 
     * @param consultantId Consultant identifier for logging
     * @param displayName Human-readable name for the file
     * @param markdown CV content in Markdown format
     * @return File URI for use in generateContent calls
     */
    suspend fun uploadCvMarkdown(
        consultantId: String,
        displayName: String,
        markdown: String
    ): String {
        log.debug { "Uploading CV for consultant $consultantId: $displayName" }
        
        val bytes = markdown.toByteArray(Charsets.UTF_8)
        val numBytes = bytes.size
        
        // Step 1: Initiate resumable upload
        val startResponse = geminiWebClient.post()
            .uri("/upload/v1beta/files?key=${geminiProperties.apiKey}")
            .header("X-Goog-Upload-Protocol", "resumable")
            .header("X-Goog-Upload-Command", "start")
            .header("X-Goog-Upload-Header-Content-Length", numBytes.toString())
            .header("X-Goog-Upload-Header-Content-Type", "text/markdown")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("file" to mapOf("display_name" to displayName)))
            .retrieve()
            .toBodilessEntity()
            .timeout(uploadTimeout)
            .awaitSingle()

        val uploadUrl = startResponse.headers.getFirst("X-Goog-Upload-URL")
            ?: error("Missing X-Goog-Upload-URL header in resumable upload response")

        log.debug { "Got upload URL for $consultantId" }

        // Step 2: Upload bytes
        val fileInfo = geminiWebClient.post()
            .uri(uploadUrl)
            .header("Content-Length", numBytes.toString())
            .header("X-Goog-Upload-Offset", "0")
            .header("X-Goog-Upload-Command", "upload, finalize")
            .bodyValue(bytes)
            .retrieve()
            .awaitBody<GeminiFileUploadResponse>()

        log.info { "Successfully uploaded CV for $consultantId -> ${fileInfo.file.uri}" }
        return fileInfo.file.uri
    }

    /**
     * Ranks candidates using Gemini with file references (Long Context approach).
     * 
     * Builds a prompt with text description + file_data parts for each CV.
     * Uses responseMimeType: application/json for structured output.
     * 
     * @param projectRequestId Project request identifier
     * @param projectDescription Customer requirements and project details
     * @param fileUris List of file URIs from uploadCvMarkdown
     * @param topN Number of top candidates to return
     * @return Ranked candidates with scores and reasons
     */
    suspend fun rankCandidatesWithFiles(
        projectRequestId: String,
        projectDescription: String,
        fileUris: List<String>,
        topN: Int
    ): RankedCandidatesResult {
        log.info { "Ranking ${fileUris.size} candidates with Files API for project $projectRequestId" }
        
        val prompt = buildRankingPrompt(projectDescription, topN)
        val parts = mutableListOf<Map<String, Any>>(
            mapOf("text" to prompt)
        )
        
        // Add file references
        fileUris.forEach { uri ->
            parts.add(mapOf(
                "file_data" to mapOf(
                    "mime_type" to "text/markdown",
                    "file_uri" to uri
                )
            ))
        }

        val request = mapOf(
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to parts
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to 0.2,
                "topP" to 0.9,
                "topK" to 40,
                "maxOutputTokens" to 2048,
                "responseMimeType" to "application/json"
            )
        )

        // Try with primary matching model first (gemini-2.5-pro)
        var model = geminiProperties.matchingModel
        var attempt = 1
        val maxAttempts = 2

        while (attempt <= maxAttempts) {
            try {
                log.info { "Ranking attempt $attempt/$maxAttempts with model: $model" }
                
                val response: Map<String, Any> = geminiWebClient.post()
                    .uri("/v1beta/models/$model:generateContent?key=${geminiProperties.apiKey}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .awaitBody()

                val textResponse = extractTextFromResponse(response)
                val parsed = objectMapper.readValue(textResponse, RankedCandidatesResult::class.java)
                    .copy(projectRequestId = projectRequestId)

                log.info { "Successfully ranked ${parsed.ranked.size} candidates with $model" }
                return parsed.copy(ranked = parsed.ranked.take(topN))
                
            } catch (e: Exception) {
                val errorMessage = e.message ?: ""
                val is503Overload = errorMessage.contains("503") || 
                                   errorMessage.contains("overloaded") || 
                                   errorMessage.contains("ServerException")
                
                if (is503Overload && attempt == 1) {
                    // Fallback to flashModel (gemini-2.5-flash) on first 503
                    log.warn { "Model $model overloaded (503), falling back to ${geminiProperties.flashModel}" }
                    model = geminiProperties.flashModel
                    attempt++
                    kotlinx.coroutines.delay(1000) // Wait 1 second before retry
                    continue
                } else {
                    log.error(e) { "Failed to rank with $model after $attempt attempts" }
                    // Graceful degradation - return empty list
                    return RankedCandidatesResult(projectRequestId = projectRequestId, ranked = emptyList())
                }
            }
        }

        // Should not reach here, but safety fallback
        return RankedCandidatesResult(projectRequestId = projectRequestId, ranked = emptyList())
    }

    private fun buildRankingPrompt(projectDescription: String, topN: Int): String = """
        Du er en teknisk rekrutteringsassistent som skal rangere konsulenter mot kundeforespørsler.
        
        Du får:
        1. En kundeforespørsel med krav og beskrivelse
        2. Flere konsulent-CVer som Markdown-filer (file_data references)
        
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
        
        VIKTIG: Basér vurderingen på CV-innholdet i filene, ikke på generell kunnskap.
        
        ================================================================================
        
        KUNDEFORESPØRSEL:
        $projectDescription
        
        ================================================================================
        
        OPPGAVE:
        Ranger de $topN best egnede konsulentene for denne kundeforespørselen.
        
        Vurder:
        1. Match mot krav (må-krav > bør-krav)
        2. Relevant erfaring fra CV
        3. Teknisk kompetanse
        4. Bransjeerfaring
        
        Returner KUN JSON som beskrevet over.
    """.trimIndent()

    private fun extractTextFromResponse(response: Map<String, Any>): String {
        val candidates = response["candidates"] as? List<*>
            ?: throw IllegalStateException("No candidates in Gemini response")
        
        val firstCandidate = candidates.firstOrNull() as? Map<*, *>
            ?: throw IllegalStateException("Empty candidates list in Gemini response")
        
        val content = firstCandidate["content"] as? Map<*, *>
            ?: throw IllegalStateException("No content in Gemini candidate")
        
        val parts = content["parts"] as? List<*>
            ?: throw IllegalStateException("No parts in Gemini content")
        
        val firstPart = parts.firstOrNull() as? Map<*, *>
            ?: throw IllegalStateException("No first part in Gemini content")
        
        return firstPart["text"] as? String
            ?: throw IllegalStateException("No text in Gemini content part")
    }
}

// Data classes for API interaction
data class GeminiFileUploadResponse(
    val file: GeminiFileInfo
)

data class GeminiFileInfo(
    val name: String,
    val uri: String,
    @JsonProperty("mimeType") val mimeType: String,
    @JsonProperty("sizeBytes") val sizeBytes: String,
    val state: String
)

data class RankedCandidatesResult(
    val projectRequestId: String,
    val ranked: List<RankedCandidateDto>
)

data class RankedCandidateDto(
    val consultantId: String,
    val score: Int,
    val reasons: List<String>
)
