package no.cloudberries.candidatematch.matches.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import no.cloudberries.candidatematch.application.ports.CandidateSnapshot
import no.cloudberries.candidatematch.application.ports.GeminiMatchingPort
import no.cloudberries.candidatematch.config.MatchingProperties
import no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto
import no.cloudberries.candidatematch.domain.ProjectRequest
import no.cloudberries.candidatematch.matches.domain.MatchCandidateResult
import no.cloudberries.candidatematch.matches.domain.ProjectMatchResult
import no.cloudberries.candidatematch.matches.domain.RequirementsExtractor
import no.cloudberries.candidatematch.service.ProjectRequestService
import no.cloudberries.candidatematch.service.consultants.ConsultantWithCvService
import no.cloudberries.candidatematch.service.matching.ConsultantScoringService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * Gemini File Search-based matching strategy.
 * Uses Gemini's managed RAG to rank candidates against project requirements.
 * 
 * Activated when matching.provider=GEMINI in application.yaml
 */
@Service
@ConditionalOnProperty(prefix = "matching", name = ["provider"], havingValue = "GEMINI")
class GeminiMatchingStrategy(
    private val geminiMatchingPort: GeminiMatchingPort,
    private val consultantWithCvService: ConsultantWithCvService,
    private val consultantScoringService: ConsultantScoringService,
    private val projectRequestService: ProjectRequestService,
    private val matchingProperties: MatchingProperties,
    private val objectMapper: ObjectMapper
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Computes matches using Gemini File Search.
     * This is a batch operation that ranks all candidates at once.
     * 
     * @param projectRequest the domain project request
     * @param matchResult the match result entity to populate
     * @return list of candidate results with scores and explanations
     */
    suspend fun computeMatches(
        projectRequest: ProjectRequest,
        matchResult: ProjectMatchResult
    ): List<MatchCandidateResult> {
        logger.info { "Computing matches using Gemini File Search for project request ${projectRequest.id?.value}" }
        
        // Step 1: Get candidate pool
        val consultants = getConsultantsForMatching(projectRequest)
        if (consultants.isEmpty()) {
            logger.warn { "No consultants available for matching" }
            return emptyList()
        }
        
        logger.info { "Retrieved ${consultants.size} consultants for Gemini evaluation" }
        
        // Step 2: Convert to candidate snapshots with CV text from database
        val candidateSnapshots = consultants.map { consultant ->
            CandidateSnapshot(
                consultantId = consultant.id?.toString() ?: "unknown",
                cvGeminiUri = formatCvText(consultant), // CV text embedded as "URI" (actually full text)
                cvQuality = extractCvQuality(consultant),
                skills = consultant.skills.map { it },
                name = consultant.name
            )
        }
        
        // Step 3: Build project description
        val projectDescription = RequirementsExtractor.from(projectRequest)
        
        // Step 4: Call Gemini for ranking
        val rankedCandidates = geminiMatchingPort.rankCandidates(
            projectRequestId = projectRequest.id?.value?.toString() ?: "unknown",
            projectDescription = projectDescription,
            candidates = candidateSnapshots,
            topN = matchingProperties.topN
        )
        
        logger.info { "Gemini ranked ${rankedCandidates.size} candidates" }
        
        // Step 5: Convert to MatchCandidateResult entities
        return rankedCandidates.mapNotNull { ranked ->
            val consultantId = ranked.consultantId.toLongOrNull()
            if (consultantId == null) {
                logger.warn { "Invalid consultant ID: ${ranked.consultantId}" }
                return@mapNotNull null
            }
            
            // Convert 0-100 score to 0.0-1.0 decimal
            val scoreDecimal = BigDecimal.valueOf(ranked.score.toDouble()).divide(BigDecimal.valueOf(100))
                .coerceIn(BigDecimal.ZERO, BigDecimal.ONE)
            
            // Serialize reasons as JSON array string
            val reasonsJson = objectMapper.writeValueAsString(ranked.reasons)
            
            MatchCandidateResult(
                matchResult = matchResult,
                consultantId = consultantId,
                matchScore = scoreDecimal,
                matchExplanation = reasonsJson
            )
        }
    }
    
    /**
     * Gets consultants suitable for matching based on project requirements.
     * Same logic as original implementation - filters by skills and CV quality.
     */
    private fun getConsultantsForMatching(projectRequest: ProjectRequest): List<ConsultantWithCvDto> {
        val requiredSkills = projectRequest.requiredSkills.map { it.name }
        
        logger.info { 
            "Selecting consultants for project ${projectRequest.id?.value}. " +
            "Required skills: ${requiredSkills.joinToString(", ")}" 
        }
        
        // Get expanded pool of consultants based on skills (~30 candidates)
        val candidatePool = if (requiredSkills.isNotEmpty()) {
            consultantWithCvService.getTopConsultantsBySkills(requiredSkills, limit = 50)
        } else {
            consultantWithCvService.getAllConsultantsWithCvs(onlyActiveCv = true).take(30)
        }
        
        if (candidatePool.isEmpty()) {
            logger.warn { "No consultants available for matching" }
            return emptyList()
        }
        
        logger.info { "Retrieved ${candidatePool.size} consultants from initial pool" }
        
        // Score and rank consultants by combined skill (50%) + CV quality (50%)
        // Select top 10 for batch AI evaluation
        val selectedConsultants = consultantScoringService.scoreConsultantsByCombinedRelevance(
            consultants = candidatePool,
            requiredSkills = requiredSkills,
            minCandidates = 10,
            maxCandidates = 10 // Exactly 10 for batch evaluation
        )
        
        logger.info { 
            "Selected ${selectedConsultants.size} consultants for Gemini matching: " +
            selectedConsultants.take(3).joinToString { it.name } +
            if (selectedConsultants.size > 3) "..." else ""
        }
        
        return selectedConsultants
    }
    
    /**
     * Extracts CV quality score from consultant data.
     * Returns a value between 0-100.
     */
    private fun extractCvQuality(consultant: ConsultantWithCvDto): Int {
        // Future: could fetch actual CV quality score from database
        // For now, use heuristic based on CV completeness
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
    
    /**
     * Formats consultant CV data into structured text for Gemini.
     * This text is embedded directly in the API request.
     */
    private fun formatCvText(consultant: ConsultantWithCvDto): String {
        return buildString {
            appendLine("=== CV for ${consultant.name} ===")
            appendLine()
            
            // Skills
            if (consultant.skills.isNotEmpty()) {
                appendLine("KOMPETANSE:")
                consultant.skills.forEach { skill ->
                    appendLine("• $skill")
                }
                appendLine()
            }
            
            // Process each CV
            consultant.cvs.forEach { cv ->
                // Key qualifications
                if (cv.keyQualifications.isNotEmpty()) {
                    appendLine("NØKKELKVALIFIKASJONER:")
                    cv.keyQualifications.forEach { kq ->
                        if (kq.label?.isNotBlank() == true) {
                            appendLine("${kq.label}:")
                        }
                        if (kq.description?.isNotBlank() == true) {
                            appendLine("  ${kq.description}")
                        }
                    }
                    appendLine()
                }
                
                // Project experience - most relevant
                if (cv.projectExperience.isNotEmpty()) {
                    appendLine("PROSJEKTERFARING:")
                    cv.projectExperience.take(5).forEach { pe ->
                        appendLine("Kunde: ${pe.customer ?: "Ikke oppgitt"}")
                        if (pe.roles.isNotEmpty()) {
                            appendLine("  Roller: ${pe.roles.mapNotNull { it.name }.joinToString(", ")}")
                        }
                        appendLine("  Periode: ${pe.fromYearMonth ?: "?"} - ${pe.toYearMonth ?: "Pågående"}")
                        if (pe.description?.isNotBlank() == true) {
                            appendLine("  Beskrivelse: ${pe.description}")
                        }
                        if (pe.longDescription?.isNotBlank() == true && pe.longDescription != pe.description) {
                            appendLine("  Detaljer: ${pe.longDescription.take(500)}")
                        }
                        if (pe.skills.isNotEmpty()) {
                            appendLine("  Teknologier: ${pe.skills.joinToString(", ")}")
                        }
                        appendLine()
                    }
                }
                
                // Work experience
                if (cv.workExperience.isNotEmpty()) {
                    appendLine("ARBEIDSERFARING:")
                    cv.workExperience.take(3).forEach { we ->
                        appendLine("${we.employer ?: "Ikke oppgitt"}")
                        appendLine("  Periode: ${we.fromYearMonth ?: "?"} - ${we.toYearMonth ?: "Nåværende"}")
                    }
                    appendLine()
                }
                
                // Education
                if (cv.education.isNotEmpty()) {
                    appendLine("UTDANNING:")
                    cv.education.forEach { edu ->
                        appendLine("• ${edu.degree ?: "Grad"} fra ${edu.school ?: "Ukjent institusjon"}")
                    }
                    appendLine()
                }
            }
        }.trim()
    }
}
