package no.cloudberries.candidatematch.matches.repository

import no.cloudberries.candidatematch.matches.domain.ProjectMatchResult
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

/**
 * Repository interface for ProjectMatchResult entities.
 * 
 * Follows DDD principles by providing only necessary data access methods
 * and keeps the repository focused on persistence concerns.
 */
@Repository
interface ProjectMatchResultRepository : JpaRepository<ProjectMatchResult, Long> {
    
    /**
     * Finds the most recent match result for a given project request.
     * 
     * @param projectRequestId the ID of the project request
     * @return the most recent ProjectMatchResult or null if none exists
     */
    @Timed
    @Query("""
        SELECT pmr FROM ProjectMatchResult pmr 
        WHERE pmr.projectRequestId = :projectRequestId 
        ORDER BY pmr.createdAt DESC
        LIMIT 1
    """)
    fun findMostRecentByProjectRequestId(@Param("projectRequestId") projectRequestId: Long): ProjectMatchResult?
    
    /**
     * Finds all match results for a given project request ordered by creation date descending.
     * 
     * @param projectRequestId the ID of the project request
     * @return list of ProjectMatchResult entities
     */
    @Timed
    fun findByProjectRequestIdOrderByCreatedAtDesc(projectRequestId: Long): List<ProjectMatchResult>
    
    /**
     * Checks if a match result exists for the given project request.
     * 
     * @param projectRequestId the ID of the project request
     * @return true if a match result exists, false otherwise
     */
    @Timed
    fun existsByProjectRequestId(projectRequestId: Long): Boolean
    
    /**
     * Finds match results created after the specified date.
     * Useful for finding recent computations.
     * 
     * @param after the cutoff date
     * @return list of ProjectMatchResult entities created after the date
     */
    @Timed
    fun findByCreatedAtAfter(after: OffsetDateTime): List<ProjectMatchResult>
    
    /**
     * Deletes all match results for a given project request.
     * Useful when recomputing matches.
     * 
     * @param projectRequestId the ID of the project request
     * @return the number of deleted records
     */
    @Timed
    fun deleteByProjectRequestId(projectRequestId: Long): Int
    
    /**
     * Counts the total number of match results for a project request.
     * 
     * @param projectRequestId the ID of the project request
     * @return the count of match results
     */
    @Timed
    fun countByProjectRequestId(projectRequestId: Long): Long
    
    /**
     * Finds projects that have match results with candidates above a certain score threshold.
     * 
     * @param minScore the minimum match score threshold
     * @return list of project request IDs with high-scoring matches
     */
    @Timed
    @Query("""
        SELECT DISTINCT pmr.projectRequestId FROM ProjectMatchResult pmr 
        JOIN pmr.candidateResults mcr 
        WHERE mcr.matchScore >= :minScore
    """)
    fun findProjectRequestIdsWithHighScoringMatches(@Param("minScore") minScore: java.math.BigDecimal): List<Long>
}