package no.cloudberries.candidatematch.service.matching

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.cloudberries.candidatematch.config.GeminiProperties
import no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto
import no.cloudberries.candidatematch.controllers.matching.MatchConsultantDto
import no.cloudberries.candidatematch.domain.ProjectRequest
import no.cloudberries.candidatematch.infrastructure.gemini.GeminiFilesApiAdapter
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ConsultantCvRepository
import no.cloudberries.candidatematch.service.consultants.ConsultantWithCvService
import org.springframework.stereotype.Service

/**
 * Service for matching consultants to project requests using Gemini Files API.
 * 
 * Implements hybrid approach:
 * 1. Database grovsortering (SQL-based skill matching) → ~50 candidates
 * 2. Combined scoring (50% skills + 50% CV quality) → top 10 candidates
 * 3. Upload CVs to Gemini Files API (with caching)
 * 4. Single batch ranking call with file references
 * 
 * Benefits of Files API approach:
 * - Smaller request payload (URIs instead of full CV text)
 * - Better context caching if same CVs used across requests
 * - Files expire after 48 hours but cache remains for re-upload detection
 */
@Service
class GeminiFilesMatchingService(
    private val geminiProperties: GeminiProperties,
    private val geminiFilesAdapter: GeminiFilesApiAdapter,
    private val consultantWithCvService: ConsultantWithCvService,
    private val consultantScoringService: ConsultantScoringService,
    private val consultantCvRepository: ConsultantCvRepository
) {
    private val log = KotlinLogging.logger {}

    /**
     * Matches consultants to a project request using Gemini Files API.
     * 
     * This is a coroutine-based async method that can be called from
     * @Async Spring methods or background jobs.
     * 
     * @param projectRequest The project request with requirements
     * @param requiredSkills Skills extracted from the request
     * @param topN Number of top candidates to return (default: 5)
     * @return Ranked list of consultant matches with scores and reasons
     */
    suspend fun matchConsultantsWithFilesApi(
        projectRequest: ProjectRequest,
        requiredSkills: List<String>,
        topN: Int = 5
    ): List<MatchConsultantDto> = withContext(Dispatchers.IO) {
        
        log.info { "[FILES API] Starting match for project ${projectRequest.id?.value} (topN=$topN)" }
        
        if (!geminiProperties.useFilesApi) {
            log.warn { "[FILES API] Files API disabled in configuration, returning empty" }
            return@withContext emptyList()
        }

        // Step 1: Fetch candidate pool via SQL (30-50 candidates)
        log.info { "[STEP 1] Fetching candidate pool with ${requiredSkills.size} required skills" }
        val candidatePool = if (requiredSkills.isNotEmpty()) {
            consultantWithCvService.getTopConsultantsBySkills(requiredSkills, limit = 50)
        } else {
            consultantWithCvService.getAllConsultantsWithCvs(onlyActiveCv = true).take(30)
        }

        if (candidatePool.isEmpty()) {
            log.warn { "[STEP 1] No consultants found in candidate pool" }
            return@withContext emptyList()
        }
        
        log.info { "[STEP 1] Retrieved ${candidatePool.size} consultants from database" }

        // Step 2: Score and select top 10 by 50% skills + 50% CV quality
        log.info { "[STEP 2] Scoring consultants by 50% skills + 50% CV quality" }
        val selectedConsultants = consultantScoringService.scoreConsultantsByCombinedRelevance(
            consultants = candidatePool,
            requiredSkills = requiredSkills,
            minCandidates = minOf(10, candidatePool.size),
            maxCandidates = 10
        )

        if (selectedConsultants.isEmpty()) {
            log.warn { "[STEP 2] No consultants selected after scoring" }
            return@withContext emptyList()
        }

        log.info { "[STEP 2] Selected ${selectedConsultants.size} consultants for Gemini evaluation" }
        log.info { "[STEP 2] Selected consultants: ${selectedConsultants.joinToString(", ") { it.name }}" }

        // Step 3: Upload CVs to Gemini (with caching)
        log.info { "[STEP 3] Uploading CVs to Gemini Files API (with caching)" }
        val fileUris = selectedConsultants.mapNotNull { consultantDto ->
            try {
                getOrUploadCvFile(consultantDto)
            } catch (e: Exception) {
                log.error(e) { "[STEP 3] Failed to upload CV for ${consultantDto.name}, skipping" }
                null
            }
        }

        if (fileUris.isEmpty()) {
            log.error { "[STEP 3] No CVs could be uploaded to Gemini" }
            return@withContext emptyList()
        }

        log.info { "[STEP 3] Prepared ${fileUris.size} file URIs for Gemini" }

        // Step 4: Build project description
        val projectDescription = buildProjectDescription(projectRequest)

        // Step 5: Call Gemini ONCE with all file references
        log.info { "[STEP 4] Calling Gemini API with ${fileUris.size} file references in SINGLE request" }
        val rankedResult = try {
            geminiFilesAdapter.rankCandidatesWithFiles(
                projectRequestId = projectRequest.id?.value?.toString() ?: "unknown",
                projectDescription = projectDescription,
                fileUris = fileUris,
                topN = topN
            )
        } catch (e: Exception) {
            log.error(e) { "[STEP 4] Gemini ranking failed" }
            return@withContext emptyList()
        }

        log.info { "[STEP 4] Gemini returned ${rankedResult.ranked.size} ranked candidates" }

        // Step 6: Map to MatchConsultantDto
        val results = rankedResult.ranked.mapNotNull { ranked ->
            val consultant = selectedConsultants.find { it.id?.toString() == ranked.consultantId }
            consultant?.let {
                MatchConsultantDto(
                    userId = it.userId,
                    name = it.name,
                    cvId = it.cvId,
                    relevanceScore = ranked.score.toDouble(),
                    justification = ranked.reasons.joinToString(". ")
                )
            }
        }

        log.info { "[RESULT] Returning ${results.size} matched consultants" }
        results.forEach { match ->
            log.info { "[RESULT] ${match.name} - Score: ${match.relevanceScore}/100" }
        }

        return@withContext results
    }

    /**
     * Gets cached file URI or uploads CV to Gemini Files API.
     * 
     * Caching strategy:
     * 1. Check if consultant_cv.gemini_file_uri exists
     * 2. If yes, return cached URI
     * 3. If no, convert to Markdown, upload, cache URI, return URI
     * 
     * Note: Files expire after 48 hours on Gemini side, but we keep the cache
     * for quick re-upload detection. If Gemini returns 404, we can re-upload.
     */
    private suspend fun getOrUploadCvFile(consultantDto: ConsultantWithCvDto): String {
        // Check cache in database
        val cvEntities = consultantCvRepository.findByConsultantId(consultantDto.id ?: -1)
        val cvEntity = cvEntities.firstOrNull()
        val cachedUri = cvEntity?.geminiFileUri
        
        if (!cachedUri.isNullOrBlank()) {
            log.debug { "Using cached file URI for ${consultantDto.name}" }
            return cachedUri
        }

        // Convert DTO to simple Markdown (inline conversion)
        val markdown = buildSimpleMarkdown(consultantDto)
        
        // Upload to Gemini
        val fileUri = geminiFilesAdapter.uploadCvMarkdown(
            consultantId = consultantDto.id?.toString() ?: "unknown",
            displayName = "CV - ${consultantDto.name}",
            markdown = markdown
        )

        // Cache URI in database
        if (cvEntity != null) {
            cvEntity.geminiFileUri = fileUri
            consultantCvRepository.save(cvEntity)
            log.debug { "Cached file URI for ${consultantDto.name}: $fileUri" }
        } else {
            log.warn { "No CV entity found for consultant ${consultantDto.id}, cannot cache URI" }
        }

        return fileUri
    }
    
    /**
     * Builds simple Markdown from ConsultantWithCvDto.
     * This is a simplified version for the Files API.
     */
    private fun buildSimpleMarkdown(consultant: ConsultantWithCvDto): String {
        return buildString {
            appendLine("# ${consultant.name}")
            appendLine()
            appendLine("## Informasjon")
            appendLine("- **ID**: ${consultant.userId}")
            appendLine()
            appendLine("## Ferdigheter")
            if (consultant.skills.isNotEmpty()) {
                consultant.skills.forEach { skill ->
                    appendLine("- **$skill**")
                }
            } else {
                appendLine("Ingen ferdigheter registrert.")
            }
            appendLine()
            
            // Add any CV text if available (simplified)
            appendLine("## CV")
            appendLine("Se kompetanseprofil og erfaring over.")
        }
    }

    /**
     * Builds a project description from ProjectRequest domain model.
     */
    private fun buildProjectDescription(projectRequest: ProjectRequest): String {
        return buildString {
            appendLine("**Kunde**: ${projectRequest.customerName}")
            appendLine()
            appendLine("**Beskrivelse**:")
            appendLine(projectRequest.requestDescription)
            appendLine()
            appendLine("**Påkrevde ferdigheter**:")
            projectRequest.requiredSkills.forEach { skill ->
                appendLine("- ${skill.name}")
            }
            appendLine()
            appendLine("**Startdato**: ${projectRequest.startDate}")
            appendLine("**Sluttdato**: ${projectRequest.endDate}")
            appendLine("**Svarfrist**: ${projectRequest.responseDeadline}")
        }
    }
}
