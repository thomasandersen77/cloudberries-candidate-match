package no.cloudberries.candidatematch.matches.repository

import no.cloudberries.candidatematch.matches.domain.MatchCandidateResult
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * Repository interface for MatchCandidateResult entities.
 * 
 * Provides specialized query methods for candidate match results,
 * focusing on score-based filtering and ranking operations.
 */
@Repository
interface MatchCandidateResultRepository : JpaRepository<MatchCandidateResult, Long> {
    
    /**
     * Finds top candidate results for a match result, ordered by score descending.
     * 
     * @param matchResultId the ID of the parent match result
     * @param limit the maximum number of results to return
     * @return list of top MatchCandidateResult entities
     */
    @Timed
    @Query("""
        SELECT mcr FROM MatchCandidateResult mcr 
        WHERE mcr.matchResult.id = :matchResultId 
        ORDER BY mcr.matchScore DESC, mcr.createdAt DESC
        LIMIT :limit
    """)
    fun findTopByMatchResultIdOrderByScoreDesc(
        @Param("matchResultId") matchResultId: Long, 
        @Param("limit") limit: Int
    ): List<MatchCandidateResult>
    
    /**
     * Finds all candidate results for a match result with pagination support.
     * 
     * @param matchResultId the ID of the parent match result
     * @param pageable pagination information
     * @return page of MatchCandidateResult entities
     */
    @Timed
    fun findByMatchResultIdOrderByMatchScoreDesc(
        matchResultId: Long, 
        pageable: Pageable
    ): Page<MatchCandidateResult>
    
    /**
     * Finds candidate results above a minimum score threshold.
     * 
     * @param matchResultId the ID of the parent match result
     * @param minScore the minimum score threshold
     * @return list of MatchCandidateResult entities above the threshold
     */
    @Timed
    @Query("""
        SELECT mcr FROM MatchCandidateResult mcr 
        WHERE mcr.matchResult.id = :matchResultId 
        AND mcr.matchScore >= :minScore 
        ORDER BY mcr.matchScore DESC
    """)
    fun findByMatchResultIdAndScoreAbove(
        @Param("matchResultId") matchResultId: Long,
        @Param("minScore") minScore: BigDecimal
    ): List<MatchCandidateResult>
    
    /**
     * Finds all candidate results for a specific consultant across all match results.
     * 
     * @param consultantId the ID of the consultant
     * @return list of MatchCandidateResult entities for the consultant
     */
    @Timed
    fun findByConsultantIdOrderByCreatedAtDesc(consultantId: Long): List<MatchCandidateResult>
    
    /**
     * Gets the average match score for a specific consultant across all matches.
     * 
     * @param consultantId the ID of the consultant
     * @return the average match score or null if no matches exist
     */
    @Timed
    @Query("""
        SELECT AVG(mcr.matchScore) FROM MatchCandidateResult mcr 
        WHERE mcr.consultantId = :consultantId
    """)
    fun getAverageScoreForConsultant(@Param("consultantId") consultantId: Long): BigDecimal?
    
    /**
     * Finds the highest scoring candidate result for a specific match result.
     * 
     * @param matchResultId the ID of the parent match result
     * @return the MatchCandidateResult with the highest score or null if none exist
     */
    @Timed
    @Query("""
        SELECT mcr FROM MatchCandidateResult mcr 
        WHERE mcr.matchResult.id = :matchResultId 
        ORDER BY mcr.matchScore DESC, mcr.createdAt DESC
        LIMIT 1
    """)
    fun findTopScorerByMatchResultId(@Param("matchResultId") matchResultId: Long): MatchCandidateResult?
    
    /**
     * Counts candidate results above a certain score threshold for a match result.
     * 
     * @param matchResultId the ID of the parent match result
     * @param minScore the minimum score threshold
     * @return count of candidates above the threshold
     */
    @Timed
    @Query("""
        SELECT COUNT(mcr) FROM MatchCandidateResult mcr 
        WHERE mcr.matchResult.id = :matchResultId 
        AND mcr.matchScore >= :minScore
    """)
    fun countByMatchResultIdAndScoreAbove(
        @Param("matchResultId") matchResultId: Long,
        @Param("minScore") minScore: BigDecimal
    ): Long
    
    /**
     * Deletes all candidate results for a specific match result.
     * Used when recomputing matches.
     * 
     * @param matchResultId the ID of the parent match result
     * @return the number of deleted records
     */
    @Timed
    fun deleteByMatchResultId(matchResultId: Long): Int
}