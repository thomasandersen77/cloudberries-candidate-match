package no.cloudberries.candidatematch.matches.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.infrastructure.repositories.ProjectRequestRepository
import no.cloudberries.candidatematch.matches.domain.MatchCandidateResult
import no.cloudberries.candidatematch.matches.domain.ProjectMatchResult
import no.cloudberries.candidatematch.matches.dto.MatchCandidateDto
import no.cloudberries.candidatematch.matches.dto.MatchTop10Response
import no.cloudberries.candidatematch.matches.dto.ProjectRequestSummaryDto
import no.cloudberries.candidatematch.matches.repository.MatchCandidateResultRepository
import no.cloudberries.candidatematch.matches.repository.ProjectMatchResultRepository
import no.cloudberries.candidatematch.service.consultants.ConsultantWithCvService
import no.cloudberries.candidatematch.service.matching.CandidateMatchingService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Implementation of ProjectMatchingService following DDD principles and Clean Architecture.
 * 
 * This service orchestrates the matching domain logic while delegating to specialized services
 * for AI analysis, consultant retrieval, and data persistence. It follows SOLID principles
 * by depending on abstractions and maintaining single responsibility.
 */
@Service
class ProjectMatchingServiceImpl(
    private val projectMatchResultRepository: ProjectMatchResultRepository,
    private val matchCandidateResultRepository: MatchCandidateResultRepository,
    private val projectRequestRepository: ProjectRequestRepository,
    private val consultantWithCvService: ConsultantWithCvService,
    private val candidateMatchingService: CandidateMatchingService
) : ProjectMatchingService {

    private val logger = KotlinLogging.logger { }

    @Timed
    @Transactional(readOnly = true)
    override fun listProjectRequests(): List<ProjectRequestSummaryDto> {
        logger.debug { "Fetching all project requests for matching overview" }
        
        return projectRequestRepository.findAll().map { entity ->
            ProjectRequestSummaryDto(
                id = entity.id!!,
                title = entity.requestDescription?.takeIf { it.length > 50 }?.substring(0, 50) + "..." 
                    ?: entity.requestDescription ?: "Untitled Project",
                customerName = entity.customerName,
                createdAt = OffsetDateTime.now() // Use current time since entity doesn't have createdAt
            )
        }.sortedByDescending { it.createdAt }
    }

    @Timed
    @Transactional
    override suspend fun computeAndPersistMatches(
        projectRequestId: Long, 
        forceRecompute: Boolean
    ): List<MatchCandidateDto> {
        logger.info { "Computing matches for project request $projectRequestId (forceRecompute: $forceRecompute)" }
        
        // Check if matches already exist and if we should skip computation
        if (!forceRecompute && projectMatchResultRepository.existsByProjectRequestId(projectRequestId)) {
            logger.info { "Matches already exist for project $projectRequestId, returning existing results" }
            return getExistingMatchCandidates(projectRequestId)
        }
        
        // Fetch project request details
        val projectRequest = projectRequestRepository.findById(projectRequestId).orElse(null)
            ?: throw IllegalArgumentException("Project request not found: $projectRequestId")
        
        // Get consultants for matching
        val consultants = getConsultantsForMatching(projectRequest)
        if (consultants.isEmpty()) {
            logger.warn { "No consultants found for matching against project $projectRequestId" }
            return emptyList()
        }
        
        // Clear existing matches if recomputing
        if (forceRecompute) {
            val deletedCount = projectMatchResultRepository.deleteByProjectRequestId(projectRequestId)
            logger.info { "Deleted $deletedCount existing match results for project $projectRequestId" }
        }
        
        // Create new match result
        val matchResult = ProjectMatchResult(projectRequestId = projectRequestId)
        val savedMatchResult = projectMatchResultRepository.save(matchResult)
        
        // Compute matches in parallel for performance
        val candidateResults = computeMatchesInParallel(consultants, projectRequest, savedMatchResult)
        
        // Persist candidate results
        val savedCandidateResults = matchCandidateResultRepository.saveAll(candidateResults)
        logger.info { "Completed matching for project $projectRequestId: ${savedCandidateResults.size} candidates evaluated" }
        
        return convertToMatchCandidateDtos(savedCandidateResults, consultants)
    }

    @Timed
    @Transactional(readOnly = true)
    override fun getMatchesForProject(projectRequestId: Long): MatchTop10Response? {
        logger.debug { "Retrieving existing matches for project request $projectRequestId" }
        
        val matchResult = projectMatchResultRepository.findMostRecentByProjectRequestId(projectRequestId)
            ?: return null
            
        val projectRequest = projectRequestRepository.findById(projectRequestId).orElse(null)
            ?: return null
            
        val topCandidates = matchResult.getTopCandidates(10)
        if (topCandidates.isEmpty()) {
            return null
        }
        
        val consultants = getConsultantsByIds(topCandidates.map { it.consultantId })
        val matchCandidates = convertToMatchCandidateDtos(topCandidates, consultants)
        
        return MatchTop10Response(
            projectRequestId = projectRequestId,
            projectTitle = projectRequest.requestDescription?.takeIf { it.length > 100 }?.substring(0, 100) + "..."
                ?: projectRequest.requestDescription,
            totalMatches = matchResult.getTotalMatches(),
            matches = matchCandidates,
            lastUpdated = matchResult.updatedAt
        )
    }

    @Async
    @Timed
    override fun triggerAsyncMatching(projectRequestId: Long, forceRecompute: Boolean) {
        logger.info { "Starting async matching computation for project $projectRequestId" }
        try {
            kotlinx.coroutines.runBlocking {
                computeAndPersistMatches(projectRequestId, forceRecompute)
            }
            logger.info { "Completed async matching computation for project $projectRequestId" }
        } catch (e: Exception) {
            logger.error(e) { "Failed async matching computation for project $projectRequestId" }
        }
    }

    override fun onProjectRequestUploaded(projectRequestId: Long) {
        logger.info { "Auto-triggering matching for uploaded project request $projectRequestId" }
        triggerAsyncMatching(projectRequestId, forceRecompute = false)
    }

    /**
     * Computes matches for consultants in parallel to improve performance.
     * Uses coroutines to batch process consultants while avoiding overwhelming the AI service.
     */
    private suspend fun computeMatchesInParallel(
        consultants: List<ConsultantWithCvDto>,
        projectRequest: no.cloudberries.candidatematch.infrastructure.entities.ProjectRequestEntity,
        matchResult: ProjectMatchResult
    ): List<MatchCandidateResult> = coroutineScope {
        
        val batchSize = 5 // Process 5 consultants concurrently to avoid rate limiting
        val batches = consultants.chunked(batchSize)
        
        batches.flatMap { batch ->
            batch.map { consultant ->
                async {
                    try {
                        computeMatchForConsultant(consultant, projectRequest, matchResult)
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to compute match for consultant ${consultant.name}" }
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    /**
     * Computes the match score for a single consultant against a project request.
     * Follows SRP by focusing only on the matching computation logic.
     */
    private suspend fun computeMatchForConsultant(
        consultant: ConsultantWithCvDto,
        projectRequest: no.cloudberries.candidatematch.infrastructure.entities.ProjectRequestEntity,
        matchResult: ProjectMatchResult
    ): MatchCandidateResult? {
        
        // Build CV text from consultant data
        val cvText = buildCvTextForMatching(consultant)
        if (cvText.isBlank()) {
            logger.debug { "Skipping consultant ${consultant.name} - no CV text available" }
            return null
        }
        
        // Build project request text
        val requestText = buildProjectRequestText(projectRequest)
        
        try {
            // Call AI matching service
            val matchResponse: CandidateMatchResponse = candidateMatchingService.matchCandidate(
                aiProvider = AIProvider.GEMINI, // Use GEMINI as specified in requirements
                cv = cvText,
                request = requestText,
                consultantName = consultant.name
            )
            
            // Parse and validate score
            val score = parseScore(matchResponse.totalScore)
            if (score == null) {
                logger.warn { "Invalid score '${matchResponse.totalScore}' for consultant ${consultant.name}" }
                return null
            }
            
            // Create candidate result
            return MatchCandidateResult(
                matchResult = matchResult,
                consultantId = consultant.id ?: throw IllegalStateException("Consultant ID cannot be null"),
                matchScore = score,
                matchExplanation = matchResponse.summary?.takeIf { it.isNotBlank() }
                    ?: "Match computed based on CV analysis and project requirements"
            )
            
        } catch (e: Exception) {
            logger.error(e) { "AI matching failed for consultant ${consultant.name}" }
            return null
        }
    }

    /**
     * Gets consultants suitable for matching based on project requirements.
     * Applies business logic to select relevant candidates.
     */
    private fun getConsultantsForMatching(
        projectRequest: no.cloudberries.candidatematch.infrastructure.entities.ProjectRequestEntity
    ): List<ConsultantWithCvDto> {
        
        // Extract skills from project request if available
        val requiredSkills = projectRequest.requiredSkills?.map { it.name } ?: emptyList()
        
        return if (requiredSkills.isNotEmpty()) {
            // Get top consultants based on skill matching
            consultantWithCvService.getTopConsultantsBySkills(requiredSkills, limit = 30)
        } else {
            // Get all consultants if no specific skills are required
            consultantWithCvService.getAllConsultantsWithCvs(onlyActiveCv = true)
                .take(20) // Limit to 20 for performance
        }
    }

    private fun getConsultantsByIds(consultantIds: List<Long>): List<ConsultantWithCvDto> {
        return consultantWithCvService.getAllConsultantsWithCvs(onlyActiveCv = true)
            .filter { it.id in consultantIds }
    }

    private fun getExistingMatchCandidates(projectRequestId: Long): List<MatchCandidateDto> {
        val matchResult = projectMatchResultRepository.findMostRecentByProjectRequestId(projectRequestId)
            ?: return emptyList()
            
        val topCandidates = matchResult.getTopCandidates(10)
        val consultants = getConsultantsByIds(topCandidates.map { it.consultantId })
        
        return convertToMatchCandidateDtos(topCandidates, consultants)
    }

    private fun convertToMatchCandidateDtos(
        candidateResults: List<MatchCandidateResult>,
        consultants: List<ConsultantWithCvDto>
    ): List<MatchCandidateDto> {
        val consultantMap = consultants.associateBy { it.id }
        
        return candidateResults.mapNotNull { result ->
            val consultant = consultantMap[result.consultantId]
            if (consultant != null) {
                MatchCandidateDto(
                    consultantId = result.consultantId,
                    consultantName = consultant.name,
                    userId = consultant.userId,
                    cvId = consultant.cvId,
                    matchScore = result.matchScore,
                    matchExplanation = result.getExplanationOrDefault(),
                    createdAt = result.createdAt
                )
            } else {
                logger.warn { "Consultant ${result.consultantId} not found for match result ${result.id}" }
                null
            }
        }.sortedByDescending { it.matchScore }
    }

    private fun buildCvTextForMatching(consultant: ConsultantWithCvDto): String {
        return buildString {
            appendLine("Consultant: ${consultant.name}")
            if (consultant.skills.isNotEmpty()) {
                appendLine("Top Skills: ${consultant.skills.joinToString(", ")}")
            }
            
            consultant.cvs.forEach { cv ->
                if (cv.keyQualifications.isNotEmpty()) {
                    appendLine("Key Qualifications:")
                    cv.keyQualifications.forEach { kq ->
                        appendLine("- $kq")
                    }
                }
                
                if (cv.workExperience.isNotEmpty()) {
                    appendLine("Work Experience:")
                    cv.workExperience.take(3).forEach { we ->
                        appendLine("- ${we.employer} (${we.fromYearMonth ?: ""} - ${we.toYearMonth ?: "Present"})")
                    }
                }
                
                if (cv.projectExperience.isNotEmpty()) {
                    appendLine("Recent Project Experience:")
                    cv.projectExperience.take(3).forEach { pe ->
                        appendLine("- ${pe.customer} (${pe.fromYearMonth ?: ""} - ${pe.toYearMonth ?: "Present"})")
                        if (pe.description?.isNotBlank() == true) {
                            appendLine("  ${pe.description}")
                        }
                        if (pe.skills.isNotEmpty()) {
                            appendLine("  Skills used: ${pe.skills.joinToString(", ")}")
                        }
                    }
                }
            }
        }.trim()
    }

    private fun buildProjectRequestText(
        projectRequest: no.cloudberries.candidatematch.infrastructure.entities.ProjectRequestEntity
    ): String {
        return buildString {
            appendLine("Project Request for: ${projectRequest.customerName}")
            if (projectRequest.requestDescription?.isNotBlank() == true) {
                appendLine("Description: ${projectRequest.requestDescription}")
            }
            if (!projectRequest.requiredSkills.isNullOrEmpty()) {
                appendLine("Required Skills: ${projectRequest.requiredSkills.joinToString(", ") { it.name }}")
            }
            appendLine("Project Duration: ${projectRequest.startDate} to ${projectRequest.endDate}")
        }.trim()
    }

    /**
     * Parses AI response score which might be in various formats.
     * Handles percentage strings, decimal values, and fractions.
     */
    private fun parseScore(scoreText: String?): BigDecimal? {
        if (scoreText.isNullOrBlank()) return null
        
        return try {
            val cleaned = scoreText.trim().replace("%", "")
            val value = cleaned.toBigDecimalOrNull() ?: return null
            
            // Convert percentage to decimal if needed
            if (value > BigDecimal.ONE) {
                value.divide(BigDecimal.valueOf(100))
            } else {
                value
            }.coerceIn(BigDecimal.ZERO, BigDecimal.ONE)
        } catch (e: Exception) {
            logger.debug(e) { "Failed to parse score: '$scoreText'" }
            null
        }
    }
}