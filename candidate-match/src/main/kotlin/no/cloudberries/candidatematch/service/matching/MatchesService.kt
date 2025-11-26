package no.cloudberries.candidatematch.service.matching

import mu.KotlinLogging
import no.cloudberries.candidatematch.config.AISettings
import no.cloudberries.candidatematch.controllers.matching.CoverageStatus
import no.cloudberries.candidatematch.controllers.matching.MatchConsultantDto
import no.cloudberries.candidatematch.controllers.matching.MatchesListItemDto
import no.cloudberries.candidatematch.controllers.matching.PagedMatchesListDto
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.CustomerProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantSearchRepository
import no.cloudberries.candidatematch.infrastructure.repositories.projectrequest.CustomerProjectRequestRepository
import no.cloudberries.candidatematch.service.ai.AIService
import no.cloudberries.candidatematch.service.consultants.ConsultantWithCvService
import no.cloudberries.candidatematch.domain.consultant.RelationalSearchCriteria
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MatchesService(
    private val requestRepo: CustomerProjectRequestRepository,
    private val consultantSearchRepository: ConsultantSearchRepository,
    private val consultantWithCvService: ConsultantWithCvService,
    private val extractor: RequirementsSkillExtractor,
    private val aiService: AIService,
    private val aiSettings: AISettings,
    private val consultantScoringService: ConsultantScoringService,
    private val geminiMatchingPort: no.cloudberries.candidatematch.application.ports.GeminiMatchingPort? = null,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun listRequestsWithCoverage(page: Int, size: Int, sort: String): PagedMatchesListDto {
        val sortParts = sort.split(",")
        val rawField = sortParts.getOrNull(0)?.ifBlank { "uploadedAt" } ?: "uploadedAt"
        // Map legacy / UI-friendly sort keys to actual entity properties
        val sortField = when (rawField) {
            "date" -> "uploadedAt" // backwards compatible alias used by frontend
            "deadline" -> "deadlineDate"
            else -> rawField
        }
        val sortDir = if (sortParts.getOrNull(1)?.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(page, size, Sort.by(sortDir, sortField))

        val pageData = requestRepo.findAllBy(pageable)
        val items = pageData.content.map { req ->
            val skills = extractor.extractSkillsAny(req).toList()
            val hitCount = computeHitCount(skills)
            val (status, label) = mapCoverage(hitCount)
            MatchesListItemDto(
                id = req.id ?: -1,
                title = req.title,
                customerName = req.customerName,
                date = req.uploadedAt,
                deadlineDate = req.deadlineDate,
                hitCount = hitCount,
                coverageStatus = status,
                coverageLabel = label,
            )
        }

        return PagedMatchesListDto(
            content = items,
            totalElements = pageData.totalElements,
            totalPages = pageData.totalPages,
            currentPage = page,
            pageSize = size,
            hasNext = pageData.hasNext(),
            hasPrevious = pageData.hasPrevious(),
        )
    }

    @Transactional(readOnly = true)
    fun getTopConsultantsWithAI(requestId: Long, limit: Int = 5): List<MatchConsultantDto> {
        val clampedLimit = limit.coerceIn(1, 10)
        val request = requestRepo.findWithRequirementsById(requestId) ?: return emptyList()
        val skills = extractor.extractSkillsAny(request).toList()
        
        logger.info { "[MATCHING START] Request $requestId requires consultants with skills: ${skills.joinToString(", ")}" }
        
        // Use batch evaluation if Gemini is available
        if (geminiMatchingPort != null) {
            logger.info { "[MATCHING MODE] Using Gemini batch evaluation (new approach)" }
            return getTopConsultantsWithGeminiBatch(requestId, skills, clampedLimit)
        }
        
        // Fallback to sequential AI evaluation
        logger.warn { "[MATCHING MODE] Using sequential AI evaluation (legacy approach) - Gemini not available" }
        val preRanked = consultantWithCvService.getTopConsultantsBySkills(skills, limit = maxOf(10, clampedLimit))

        val requestText = buildRequestText(request)
        val enriched = preRanked.map { consultant ->
            val cvText = ConsultantCvTextFlattener.toText(consultant)
            val (score, justification) = matchWithAI(consultant.name, cvText, requestText)
            Pair(consultant, score to justification)
        }

        return enriched
            .sortedByDescending { it.second.first }
            .take(clampedLimit)
            .map { (c, pair) ->
                val (score, justification) = pair
                MatchConsultantDto(
                    userId = c.userId,
                    name = c.name,
                    cvId = c.cvId,
                    relevanceScore = score,
                    justification = justification,
                )
            }
    }
    
    /**
     * Gets top consultants using Gemini batch evaluation.
     * This sends all CVs in a single API call for efficient processing.
     */
    private fun getTopConsultantsWithGeminiBatch(
        requestId: Long, 
        skills: List<String>, 
        limit: Int
    ): List<MatchConsultantDto> {
        val request = requestRepo.findWithRequirementsById(requestId) ?: return emptyList()
        
        // Step 1: Get expanded pool (~30-50 consultants)
        logger.info { "[STEP 1] Fetching candidate pool with ${skills.size} required skills" }
        val candidatePool = if (skills.isNotEmpty()) {
            consultantWithCvService.getTopConsultantsBySkills(skills, limit = 50)
        } else {
            consultantWithCvService.getAllConsultantsWithCvs(onlyActiveCv = true).take(30)
        }
        
        if (candidatePool.isEmpty()) {
            logger.warn { "[STEP 1] No consultants found in candidate pool" }
            return emptyList()
        }
        
        logger.info { "[STEP 1] Retrieved ${candidatePool.size} consultants from database" }
        
        // Step 2: Score and select top 10 by 50% skills + 50% CV quality
        logger.info { "[STEP 2] Scoring consultants by 50% skills + 50% CV quality" }
        val selectedConsultants = consultantScoringService.scoreConsultantsByCombinedRelevance(
            consultants = candidatePool,
            requiredSkills = skills,
            minCandidates = minOf(10, candidatePool.size),
            maxCandidates = 10
        )
        
        if (selectedConsultants.isEmpty()) {
            logger.warn { "[STEP 2] No consultants selected after scoring" }
            return emptyList()
        }
        
        logger.info { "[STEP 2] Selected ${selectedConsultants.size} consultants for Gemini evaluation" }
        logger.info { "[STEP 2] Selected consultants: ${selectedConsultants.joinToString(", ") { it.name }}" }
        
        // Step 3: Build candidate snapshots with full CV text
        logger.info { "[STEP 3] Building candidate snapshots with CV text" }
        val candidateSnapshots = selectedConsultants.map { consultant ->
            no.cloudberries.candidatematch.application.ports.CandidateSnapshot(
                consultantId = consultant.id?.toString() ?: "unknown",
                cvGeminiUri = ConsultantCvTextFlattener.toText(consultant),
                cvQuality = extractCvQuality(consultant),
                skills = consultant.skills,
                name = consultant.name
            )
        }
        
        logger.info { "[STEP 3] Built ${candidateSnapshots.size} candidate snapshots" }
        
        // Step 4: Build project description
        val projectDescription = buildRequestText(request)
        
        // Step 5: Call Gemini ONCE for all candidates
        logger.info { "[STEP 4] Calling Gemini API with ${candidateSnapshots.size} candidates in SINGLE batch request" }
        val rankedCandidates = try {
            geminiMatchingPort?.let { port ->
                kotlinx.coroutines.runBlocking {
                    port.rankCandidates(
                        projectRequestId = requestId.toString(),
                        projectDescription = projectDescription,
                        candidates = candidateSnapshots,
                        topN = limit
                    )
                }
            } ?: emptyList()
        } catch (e: Exception) {
            logger.error(e) { "[STEP 4] Gemini batch ranking failed" }
            return emptyList()
        }
        
        logger.info { "[STEP 4] Gemini returned ${rankedCandidates.size} ranked candidates" }
        rankedCandidates.forEach { ranked ->
            val consultantName = selectedConsultants.find { it.id?.toString() == ranked.consultantId }?.name ?: ranked.consultantId
            logger.info { "[RESULT] $consultantName - Score: ${ranked.score}/100 - Reasons: ${ranked.reasons.take(2).joinToString("; ")}" }
        }
        
        // Step 6: Map to DTOs
        return rankedCandidates.mapNotNull { ranked ->
            val consultant = selectedConsultants.find { it.id?.toString() == ranked.consultantId }
            if (consultant == null) {
                logger.warn { "Could not find consultant with ID ${ranked.consultantId} in selected list" }
                return@mapNotNull null
            }
            
            MatchConsultantDto(
                userId = consultant.userId,
                name = consultant.name,
                cvId = consultant.cvId,
                relevanceScore = ranked.score.toDouble(),
                justification = ranked.reasons.joinToString("; ")
            )
        }
    }

    private fun matchWithAI(name: String, cvText: String, requestText: String): Pair<Double, String?> {
        return try {
            if (!aiSettings.enabled) return fallbackScore(name, requestText)
            val provider: AIProvider = aiSettings.provider
            val resp = aiService.matchCandidate(
                aiProvider = provider,
                cv = cvText,
                request = requestText,
                consultantName = name,
            )
            val score = resp.totalScore.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 70.0
            val justification = resp.summary
            score to justification
        } catch (e: Exception) {
            logger.warn(e) { "AI match failed for $name, using fallback score" }
            fallbackScore(name, requestText)
        }
    }

    private fun fallbackScore(name: String, requestText: String): Pair<Double, String?> {
        // We don't have structural overlap here easily; return a conservative mid score
        val score = 60.0
        val justification = "Begrunnelse basert på overlapp med krav. (Fallback – AI utilgjengelig)"
        return score to justification
    }

    private fun buildRequestText(req: CustomerProjectRequestEntity): String {
        val musts = req.requirements.mapNotNull { r -> r.name?.takeIf { it.isNotBlank() } }
        return """
            Kunde: ${req.customerName ?: "-"}
            Tittel: ${req.title ?: "-"}
            Oppsummering: ${req.summary ?: "-"}
            Krav: ${musts.joinToString(", ")}
        """.trimIndent()
    }

    private fun mapCoverage(hitCount: Long): Pair<CoverageStatus, String> = when {
        hitCount >= 10 -> CoverageStatus.GREEN to "God dekning"
        hitCount <= 2 -> CoverageStatus.RED to "Lav dekning"
        hitCount in 5..9 -> CoverageStatus.YELLOW to "Begrenset dekning"
        else -> CoverageStatus.NEUTRAL to "Nøytral"
    }

    private fun computeHitCount(skillsAny: List<String>): Long {
        if (skillsAny.isEmpty()) return 0
        val criteria = RelationalSearchCriteria(
            skillsAll = emptyList(),
            skillsAny = skillsAny,
            minQualityScore = null,
            onlyActiveCv = true,
        )
        val page = consultantSearchRepository.findByRelationalCriteria(criteria, PageRequest.of(0, 1))
        return page.totalElements
    }
    
    /**
     * Re-analyzes consultants for a project request using Gemini 3.0 batch evaluation.
     * This method runs synchronously and uses the newer batch ranking approach.
     * 
     * Steps:
     * 1. Fetch ~30 consultants matching skills
     * 2. Score by 50% skills + 50% CV quality
     * 3. Select top 10 consultants
     * 4. Send all 10 CVs in ONE Gemini API call
     * 5. Return ranked results
     */
    @Transactional(readOnly = true)
    fun reAnalyzeWithGemini(requestId: Long): List<MatchConsultantDto> {
        logger.info { "Re-analyzing consultants for request $requestId using Gemini batch evaluation" }
        
        val request = requestRepo.findWithRequirementsById(requestId)
            ?: throw IllegalArgumentException("Project request not found: $requestId")
        
        if (geminiMatchingPort == null) {
            logger.warn { "Gemini matching not available, falling back to standard AI matching" }
            return getTopConsultantsWithAI(requestId, 10)
        }
        
        // Step 1: Extract skills from request
        val skills = extractor.extractSkillsAny(request).toList()
        
        // Step 2: Get expanded pool (~30-50 consultants)
        val candidatePool = if (skills.isNotEmpty()) {
            consultantWithCvService.getTopConsultantsBySkills(skills, limit = 50)
        } else {
            consultantWithCvService.getAllConsultantsWithCvs(onlyActiveCv = true).take(30)
        }
        
        if (candidatePool.isEmpty()) {
            logger.warn { "No consultants available for re-analysis" }
            return emptyList()
        }
        
        logger.info { "Retrieved ${candidatePool.size} consultants from initial pool" }
        
        // Step 3: Score and select top 10 (50% skills + 50% CV quality)
        val top10 = consultantScoringService.scoreConsultantsByCombinedRelevance(
            consultants = candidatePool,
            requiredSkills = skills,
            minCandidates = 10,
            maxCandidates = 10
        )
        
        logger.info { "Selected top ${top10.size} consultants for batch Gemini evaluation" }
        
        // Step 4: Build candidate snapshots with full CV text
        val candidateSnapshots = top10.map { consultant ->
            no.cloudberries.candidatematch.application.ports.CandidateSnapshot(
                consultantId = consultant.id.toString(),
                cvGeminiUri = ConsultantCvTextFlattener.toText(consultant), // Full CV text
                cvQuality = extractCvQuality(consultant),
                skills = consultant.skills,
                name = consultant.name
            )
        }
        
        // Step 5: Build project description
        val projectDescription = buildRequestText(request)
        
        // Step 6: Call Gemini with batch of candidates (single API call)
        val rankedResults = try {
            kotlinx.coroutines.runBlocking {
                geminiMatchingPort.rankCandidates(
                    projectRequestId = requestId.toString(),
                    projectDescription = projectDescription,
                    candidates = candidateSnapshots,
                    topN = 10
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Gemini batch ranking failed, returning empty list" }
            return emptyList()
        }
        
        logger.info { "Gemini ranked ${rankedResults.size} consultants" }
        
        // Step 7: Map results back to DTO
        val consultantMap = top10.associateBy { it.id.toString() }
        return rankedResults.mapNotNull { ranked ->
            val consultant = consultantMap[ranked.consultantId]
            if (consultant != null) {
                MatchConsultantDto(
                    userId = consultant.userId,
                    name = consultant.name,
                    cvId = consultant.cvId,
                    relevanceScore = ranked.score.toDouble(),
                    justification = ranked.reasons.joinToString(" • ")
                )
            } else {
                logger.warn { "Consultant ${ranked.consultantId} not found in original pool" }
                null
            }
        }
    }
    
    /**
     * Extracts CV quality score from consultant data.
     */
    private fun extractCvQuality(consultant: no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto): Int {
        return when {
            consultant.cvs.isEmpty() -> 30
            consultant.cvs.any { 
                it.keyQualifications.isNotEmpty() && 
                it.workExperience.isNotEmpty() && 
                it.projectExperience.isNotEmpty() 
            } -> 90
            consultant.cvs.any { 
                it.keyQualifications.isNotEmpty() || 
                it.workExperience.isNotEmpty() 
            } -> 70
            else -> 50
        }
    }
}
