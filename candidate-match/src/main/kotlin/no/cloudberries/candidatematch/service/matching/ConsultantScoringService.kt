package no.cloudberries.candidatematch.service.matching

import mu.KotlinLogging
import no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto
import no.cloudberries.candidatematch.infrastructure.repositories.scoring.CvScoreRepository
import org.springframework.stereotype.Service

/**
 * Service responsible for scoring and ranking consultants for project matching.
 * 
 * Combines two factors:
 * - Skill match score (70% weight): How well consultant's skills match project requirements
 * - CV quality score (30% weight): Overall quality and completeness of the consultant's CV
 * 
 * This ensures that we select consultants who are both relevant to the project
 * and have well-documented experience.
 */
@Service
class ConsultantScoringService(
    private val cvScoreRepository: CvScoreRepository
) {
    
    private val logger = KotlinLogging.logger { }
    
    /**
     * Scores consultants by combining skill match and CV quality.
     * 
     * Algorithm:
     * 1. Calculate skill match score (0.0 to 1.0) for each consultant
     * 2. Fetch CV quality scores from database (normalized 0.0 to 1.0)
     * 3. Combine: combined_score = (0.7 * skill_match) + (0.3 * cv_quality)
     * 4. Sort by combined score and return top N
     * 
     * @param consultants Pre-filtered consultants (from skill-based search)
     * @param requiredSkills Skills required for the project
     * @param minCandidates Minimum number of candidates to return (default: 5)
     * @param maxCandidates Maximum number of candidates to return (default: 15)
     * @return Ranked list of consultants by combined score
     */
    fun scoreConsultantsByCombinedRelevance(
        consultants: List<ConsultantWithCvDto>,
        requiredSkills: List<String>,
        minCandidates: Int = 5,
        maxCandidates: Int = 15
    ): List<ConsultantWithCvDto> {
        
        if (consultants.isEmpty()) {
            logger.warn { "No consultants provided for scoring" }
            return emptyList()
        }
        
        logger.info { 
            "Scoring ${consultants.size} consultants with ${requiredSkills.size} required skills. " +
            "Target: $minCandidates-$maxCandidates consultants" 
        }
        
        // Step 1: Calculate skill match scores (0.0 to 1.0)
        val skillScores = consultants.map { consultant ->
            val score = calculateSkillMatchScore(consultant, requiredSkills)
            consultant to score
        }
        
        // Step 2: Fetch CV quality scores for all consultants
        val cvQualityScores = fetchCvQualityScores(consultants)
        
        // Step 3: Combine scores with weighting
        val combinedScores = skillScores.map { (consultant, skillScore) ->
            val cvQuality = cvQualityScores[consultant.userId] ?: DEFAULT_CV_SCORE
            val combinedScore = (SKILL_WEIGHT * skillScore) + (CV_QUALITY_WEIGHT * cvQuality)
            
            ScoredConsultant(consultant, combinedScore, skillScore, cvQuality)
        }
        
        // Step 4: Sort by combined score descending
        val ranked = combinedScores
            .filter { it.combinedScore >= MIN_THRESHOLD_SCORE }
            .sortedByDescending { it.combinedScore }
        
        // Step 5: Ensure minimum candidates
        val selected = when {
            ranked.size >= minCandidates -> ranked.take(maxCandidates)
            consultants.size >= minCandidates -> {
                logger.warn { 
                    "Only ${ranked.size} consultants meet threshold ($MIN_THRESHOLD_SCORE), " +
                    "expanding to $minCandidates by including lower-scored consultants" 
                }
                combinedScores.sortedByDescending { it.combinedScore }.take(minCandidates)
            }
            else -> {
                logger.warn { 
                    "Only ${consultants.size} consultants available (minimum $minCandidates requested)" 
                }
                combinedScores.sortedByDescending { it.combinedScore }
            }
        }
        
        logScoringResults(selected)
        
        return selected.map { it.consultant }
    }
    
    /**
     * Calculates skill match score as percentage of required skills covered.
     * 
     * If no skills are specified, returns a neutral score of 0.5.
     * Uses case-insensitive matching and handles partial overlaps.
     */
    private fun calculateSkillMatchScore(
        consultant: ConsultantWithCvDto,
        requiredSkills: List<String>
    ): Double {
        if (requiredSkills.isEmpty()) {
            return 0.5 // Neutral score when no specific skills required
        }
        
        val consultantSkills = consultant.skills.map { it.uppercase().trim() }.toSet()
        val required = requiredSkills.map { it.uppercase().trim() }.toSet()
        
        if (consultantSkills.isEmpty()) {
            return 0.0
        }
        
        val matchCount = consultantSkills.intersect(required).size
        val coverageRatio = matchCount.toDouble() / required.size
        
        // Bonus: If consultant has more relevant skills than required, give small bonus
        val totalRelevantSkills = consultantSkills.intersect(required).size
        val bonusMultiplier = if (totalRelevantSkills > 0) {
            1.0 + (totalRelevantSkills * 0.05) // 5% bonus per matching skill
        } else {
            1.0
        }
        
        return (coverageRatio * bonusMultiplier).coerceIn(0.0, 1.0)
    }
    
    /**
     * Fetches CV quality scores from database for batch of consultants.
     * 
     * Normalizes scores from percentage (0-100) to decimal (0.0-1.0).
     * Returns default score (0.5) for consultants without CV scores.
     */
    private fun fetchCvQualityScores(consultants: List<ConsultantWithCvDto>): Map<String, Double> {
        val userIds = consultants.mapNotNull { it.userId }
        
        if (userIds.isEmpty()) {
            logger.warn { "No user IDs found in consultants for CV score lookup" }
            return emptyMap()
        }
        
        val cvScores = cvScoreRepository.findByCandidateUserIdIn(userIds)
        
        logger.debug { "Found ${cvScores.size} CV scores for ${userIds.size} consultants" }
        
        return cvScores.associate { score ->
            val normalizedScore = score.scorePercent.toDouble() / 100.0
            score.candidateUserId to normalizedScore.coerceIn(0.0, 1.0)
        }
    }
    
    /**
     * Logs detailed scoring results for debugging and monitoring.
     */
    private fun logScoringResults(scoredConsultants: List<ScoredConsultant>) {
        if (scoredConsultants.isEmpty()) {
            logger.warn { "No consultants selected after scoring" }
            return
        }
        
        val topScore = scoredConsultants.first().combinedScore
        val bottomScore = scoredConsultants.last().combinedScore
        val avgScore = scoredConsultants.map { it.combinedScore }.average()
        
        logger.info { 
            "Selected ${scoredConsultants.size} consultants. " +
            "Scores: top=${topScore.format(3)}, bottom=${bottomScore.format(3)}, avg=${avgScore.format(3)}" 
        }
        
        // Log top 3 for visibility
        scoredConsultants.take(3).forEachIndexed { index, scored ->
            logger.debug { 
                "Rank ${index + 1}: ${scored.consultant.name} " +
                "(combined=${scored.combinedScore.format(3)}, " +
                "skill=${scored.skillScore.format(3)}, " +
                "cv=${scored.cvQualityScore.format(3)})"
            }
        }
    }
    
    /**
     * Internal data class to hold scored consultant information.
     */
    private data class ScoredConsultant(
        val consultant: ConsultantWithCvDto,
        val combinedScore: Double,
        val skillScore: Double,
        val cvQualityScore: Double
    )
    
    companion object {
        private const val SKILL_WEIGHT = 0.5  // 50% weight for skills
        private const val CV_QUALITY_WEIGHT = 0.5  // 50% weight for CV quality
        private const val MIN_THRESHOLD_SCORE = 0.2 // Minimum 20% combined relevance
        private const val DEFAULT_CV_SCORE = 0.5 // Default if no CV score exists
    }
}

/**
 * Extension function to format Double to specified decimal places.
 */
private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
