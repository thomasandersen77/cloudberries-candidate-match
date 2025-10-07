package no.cloudberries.candidatematch.matches.service

import no.cloudberries.candidatematch.matches.dto.MatchCandidateDto
import no.cloudberries.candidatematch.matches.dto.MatchTop10Response
import no.cloudberries.candidatematch.matches.dto.ProjectRequestSummaryDto

/**
 * Service interface for project request matching operations.
 * 
 * Follows DDD principles by defining the contract for matching domain operations.
 * Implements the Repository pattern with clean separation of concerns.
 */
interface ProjectMatchingService {
    
    /**
     * Lists all project requests available for matching.
     * 
     * @return list of project request summaries
     */
    fun listProjectRequests(): List<ProjectRequestSummaryDto>
    
    /**
     * Computes and persists consultant matches for a project request.
     * This is the core domain operation that orchestrates the matching process.
     * 
     * @param projectRequestId the ID of the project request
     * @param forceRecompute if true, recalculates even if matches exist
     * @return list of matched candidates with scores and explanations
     */
    suspend fun computeAndPersistMatches(
        projectRequestId: Long, 
        forceRecompute: Boolean = false
    ): List<MatchCandidateDto>
    
    /**
     * Retrieves existing matches for a project request.
     * Returns cached results without triggering computation.
     * 
     * @param projectRequestId the ID of the project request
     * @return match results with top 10 consultants or null if no matches exist
     */
    fun getMatchesForProject(projectRequestId: Long): MatchTop10Response?
    
    /**
     * Triggers asynchronous matching computation for a project request.
     * This method returns immediately while computation happens in the background.
     * 
     * @param projectRequestId the ID of the project request
     * @param forceRecompute whether to force recomputation if matches already exist
     */
    fun triggerAsyncMatching(projectRequestId: Long, forceRecompute: Boolean = false)
    
    /**
     * Event handler called when a project request is uploaded.
     * Automatically triggers matching computation in the background.
     * 
     * @param projectRequestId the ID of the newly uploaded project request
     */
    fun onProjectRequestUploaded(projectRequestId: Long)
}