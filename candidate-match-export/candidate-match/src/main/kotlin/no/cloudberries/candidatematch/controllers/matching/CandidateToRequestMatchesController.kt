package no.cloudberries.candidatematch.controllers.matching

import mu.KotlinLogging
import no.cloudberries.candidatematch.matches.dto.*
import no.cloudberries.candidatematch.matches.service.ProjectMatchingService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST controller for consultant matching operations.
 * 
 * Follows RESTful principles and delegates business logic to the service layer.
 * Provides endpoints for manual matching triggers and result retrieval.
 */
@RestController
@RequestMapping("/matches")
class CandidateToRequestMatchesController(
    private val projectMatchingService: ProjectMatchingService
) {
    
    private val logger = KotlinLogging.logger { }

    /**
     * Lists all project requests available for matching.
     * 
     * @return list of project request summaries
     */
    @GetMapping("/requests")
    @Timed
    fun listProjectRequests(): ResponseEntity<List<ProjectRequestSummaryDto>> {
        logger.debug { "Fetching all project requests for matches overview" }
        
        val projectRequests = projectMatchingService.listProjectRequests()
        return ResponseEntity.ok(projectRequests)
    }

    /**
     * Triggers matching computation for a specific project request.
     * This endpoint allows manual triggering of the matching process.
     * 
     * @param projectRequestId the ID of the project request
     * @param forceRecompute whether to recompute even if matches exist
     * @return response indicating the trigger status
     */
    @PostMapping("/requests/{projectRequestId}/trigger")
    @Timed
    fun triggerMatchingComputation(
        @PathVariable projectRequestId: Long,
        @RequestParam(defaultValue = "false") forceRecompute: Boolean
    ): ResponseEntity<TriggerMatchingResponse> {
        
        logger.info { "Manual trigger requested for project $projectRequestId (forceRecompute: $forceRecompute)" }
        
        return try {
            // Generate a job ID for tracking
            val jobId = UUID.randomUUID().toString()
            
            // Trigger async computation
            projectMatchingService.triggerAsyncMatching(projectRequestId, forceRecompute)
            
            // Return immediate response
            val response = TriggerMatchingResponse(
                projectRequestId = projectRequestId,
                status = "TRIGGERED",
                message = "Matching computation started successfully. Results will be available shortly.",
                jobId = jobId
            )
            
            ResponseEntity.ok(response)
            
        } catch (e: IllegalArgumentException) {
            logger.warn { "Invalid request for project matching: ${e.message}" }
            
            val errorResponse = TriggerMatchingResponse(
                projectRequestId = projectRequestId,
                status = "ERROR",
                message = "Invalid request: ${e.message}"
            )
            
            ResponseEntity.badRequest().body(errorResponse)
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to trigger matching for project $projectRequestId" }
            
            val errorResponse = TriggerMatchingResponse(
                projectRequestId = projectRequestId,
                status = "ERROR", 
                message = "Failed to start matching computation: ${e.message}"
            )
            
            ResponseEntity.internalServerError().body(errorResponse)
        }
    }

    /**
     * Retrieves computed matches for a project request.
     * Returns the top 10 consultant matches with scores and explanations.
     * 
     * @param projectRequestId the ID of the project request
     * @return match results with top consultants or 404 if no matches exist
     */
    @GetMapping("/requests/{projectRequestId}/top")
    @Timed
    fun getTopMatches(@PathVariable projectRequestId: Long): ResponseEntity<MatchTop10Response> {
        logger.debug { "Fetching top matches for project request $projectRequestId" }
        
        val matches = projectMatchingService.getMatchesForProject(projectRequestId)
        
        return if (matches != null) {
            ResponseEntity.ok(matches)
        } else {
            logger.debug { "No matches found for project request $projectRequestId" }
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Gets match results for a project request (alias for /top for backward compatibility).
     * 
     * @param projectRequestId the ID of the project request
     * @return match results with top consultants or 404 if no matches exist
     */
    @GetMapping("/requests/{projectRequestId}/results")
    @Timed
    fun getMatchResults(@PathVariable projectRequestId: Long): ResponseEntity<MatchTop10Response> {
        return getTopMatches(projectRequestId)
    }

    /**
     * Health check endpoint for the matching service.
     * 
     * @return service status
     */
    @GetMapping("/health")
    @Timed
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        logger.debug { "Health check requested for matches service" }
        
        return try {
            // Check if service can list project requests
            val projectCount = projectMatchingService.listProjectRequests().size
            
            val health: Map<String, Any> = mapOf(
                "status" to "UP",
                "service" to "MatchesController",
                "projectRequestsCount" to projectCount,
                "timestamp" to System.currentTimeMillis()
            )
            
            ResponseEntity.ok(health)
        } catch (e: Exception) {
            logger.error(e) { "Health check failed for matches service" }
            
            val health: Map<String, Any> = mapOf(
                "status" to "DOWN",
                "service" to "MatchesController",
                "error" to (e.message ?: "Unknown error"),
                "timestamp" to System.currentTimeMillis()
            )
            
            ResponseEntity.internalServerError().body(health)
        }
    }

    /**
     * Triggers matching for all project requests (admin operation).
     * This is useful for batch processing or system maintenance.
     * 
     * @param forceRecompute whether to recompute even if matches exist
     * @return batch operation status
     */
    @PostMapping("/trigger-all")
    @Timed
    fun triggerAllMatches(
        @RequestParam(defaultValue = "false") forceRecompute: Boolean
    ): ResponseEntity<Map<String, Any>> {
        
        logger.info { "Batch matching trigger requested (forceRecompute: $forceRecompute)" }
        
        return try {
            val projectRequests = projectMatchingService.listProjectRequests()
            val jobId = UUID.randomUUID().toString()
            
            // Trigger matching for all projects
            projectRequests.forEach { project ->
                try {
                    projectMatchingService.triggerAsyncMatching(project.id, forceRecompute)
                    logger.debug { "Triggered matching for project ${project.id}" }
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to trigger matching for project ${project.id}" }
                }
            }
            
            val response: Map<String, Any> = mapOf(
                "status" to "TRIGGERED",
                "message" to "Batch matching started for ${projectRequests.size} projects",
                "projectCount" to projectRequests.size,
                "jobId" to jobId,
                "forceRecompute" to forceRecompute,
                "timestamp" to System.currentTimeMillis()
            )
            
            ResponseEntity.ok(response)
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to trigger batch matching" }
            
            val errorResponse: Map<String, Any> = mapOf(
                "status" to "ERROR",
                "message" to "Failed to start batch matching: ${e.message}",
                "timestamp" to System.currentTimeMillis()
            )
            
            ResponseEntity.internalServerError().body(errorResponse)
        }
    }
}