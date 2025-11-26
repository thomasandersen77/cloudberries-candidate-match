package no.cloudberries.candidatematch.infrastructure.gemini

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.cloudberries.candidatematch.application.ports.CandidateSnapshot
import no.cloudberries.candidatematch.application.ports.GeminiMatchingPort
import no.cloudberries.candidatematch.application.ports.RankedCandidateDto
import no.cloudberries.candidatematch.config.GeminiProperties
import no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto
import no.cloudberries.candidatematch.domain.ProjectRequest
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ConsultantCvRepository
import no.cloudberries.candidatematch.service.consultants.ConsultantWithCvService
import no.cloudberries.candidatematch.service.matching.ConsultantScoringService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Adapter that bridges GeminiFilesMatchingService to GeminiMatchingPort.
 * 
 * This adapter enables Files API batch matching when matching.useFilesApi=true.
 * It delegates to GeminiFilesApiAdapter for actual API calls and provides
 * the necessary CV data conversion from CandidateSnapshot to ConsultantWithCvDto.
 * 
 * Activated when:
 * - matching.provider=GEMINI (via GeminiMatchingStrategy)
 * - gemini.useFilesApi=true
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "gemini", name = ["useFilesApi"], havingValue = "true")
class GeminiFilesAdapter(
    private val geminiFilesApiAdapter: GeminiFilesApiAdapter,
    private val geminiProperties: GeminiProperties,
    private val consultantWithCvService: ConsultantWithCvService,
    private val consultantScoringService: ConsultantScoringService,
    private val consultantCvRepository: ConsultantCvRepository
) : GeminiMatchingPort {

    private val logger = KotlinLogging.logger {}

    override suspend fun ensureStore(): String {
        // Files API doesn't require store setup - files are uploaded per-request
        logger.debug { "Files API ready - no store setup needed" }
        return "files-api"
    }

    override suspend fun uploadCv(fileId: String, filename: String, bytes: ByteArray): String {
        // Delegate to GeminiFilesApiAdapter for resumable upload
        val markdown = String(bytes, Charsets.UTF_8)
        return geminiFilesApiAdapter.uploadCvMarkdown(
            consultantId = fileId,
            displayName = filename,
            markdown = markdown
        )
    }

    override suspend fun rankCandidates(
        projectRequestId: String,
        projectDescription: String,
        candidates: List<CandidateSnapshot>,
        topN: Int
    ): List<RankedCandidateDto> {
        logger.info { "[FILES API] Ranking ${candidates.size} candidates for project $projectRequestId" }
        
        if (candidates.isEmpty()) {
            logger.warn { "[FILES API] No candidates provided" }
            return emptyList()
        }

        // Step 1: Upload CVs with caching (parallel for efficiency)
        val fileUris = candidates.mapNotNull { snapshot ->
            try {
                getOrUploadCvFileFromSnapshot(snapshot)
            } catch (e: Exception) {
                logger.error(e) { "[FILES API] Failed to upload CV for consultant ${snapshot.consultantId}" }
                null
            }
        }

        if (fileUris.isEmpty()) {
            logger.error { "[FILES API] No CVs could be uploaded" }
            return emptyList()
        }

        logger.info { "[FILES API] Prepared ${fileUris.size} file URIs for batch ranking" }

        // Step 2: Call Gemini ONCE with all file references (batch operation)
        val rankedResult = try {
            geminiFilesApiAdapter.rankCandidatesWithFiles(
                projectRequestId = projectRequestId,
                projectDescription = projectDescription,
                fileUris = fileUris,
                topN = topN
            )
        } catch (e: Exception) {
            logger.error(e) { "[FILES API] Gemini ranking failed" }
            return emptyList()
        }

        logger.info { "[FILES API] Gemini returned ${rankedResult.ranked.size} ranked candidates" }

        // Step 3: Convert to port DTOs
        return rankedResult.ranked.map { ranked ->
            RankedCandidateDto(
                consultantId = ranked.consultantId,
                score = ranked.score,
                reasons = ranked.reasons
            )
        }
    }

    /**
     * Gets cached file URI or uploads CV to Gemini Files API.
     * 
     * Uses CandidateSnapshot.cvGeminiUri which contains the full CV text
     * (stored by GeminiMatchingStrategy in formatCvText method).
     * 
     * Caching strategy:
     * 1. Check if consultant_cv.gemini_file_uri exists
     * 2. If yes, return cached URI
     * 3. If no, convert snapshot text to Markdown, upload, cache URI, return URI
     */
    private suspend fun getOrUploadCvFileFromSnapshot(snapshot: CandidateSnapshot): String {
        val consultantId = snapshot.consultantId.toLongOrNull()
        if (consultantId == null) {
            logger.warn { "Invalid consultant ID: ${snapshot.consultantId}" }
            throw IllegalArgumentException("Invalid consultant ID")
        }

        // Check cache in database
        val cvEntities = consultantCvRepository.findByConsultantId(consultantId)
        val cvEntity = cvEntities.firstOrNull()
        val cachedUri = cvEntity?.geminiFileUri

        if (!cachedUri.isNullOrBlank()) {
            logger.debug { "Using cached file URI for consultant $consultantId" }
            return cachedUri
        }

        // Convert snapshot's CV text (stored in cvGeminiUri field) to Markdown
        val markdown = snapshot.cvGeminiUri ?: buildFallbackMarkdown(snapshot)

        // Upload to Gemini
        val fileUri = geminiFilesApiAdapter.uploadCvMarkdown(
            consultantId = snapshot.consultantId,
            displayName = "CV - ${snapshot.name ?: snapshot.consultantId}",
            markdown = markdown
        )

        // Cache URI in database
        if (cvEntity != null) {
            cvEntity.geminiFileUri = fileUri
            consultantCvRepository.save(cvEntity)
            logger.debug { "Cached file URI for consultant $consultantId: $fileUri" }
        } else {
            logger.warn { "No CV entity found for consultant $consultantId, cannot cache URI" }
        }

        return fileUri
    }

    /**
     * Builds fallback Markdown when full CV text is not available.
     * Uses skills and name from snapshot.
     */
    private fun buildFallbackMarkdown(snapshot: CandidateSnapshot): String {
        return buildString {
            appendLine("# CV for ${snapshot.name ?: "Consultant ${snapshot.consultantId}"}")
            appendLine()
            appendLine("## Skills")
            snapshot.skills.forEach { skill ->
                appendLine("- $skill")
            }
            appendLine()
            appendLine("CV Quality: ${snapshot.cvQuality}/100")
        }
    }
}
