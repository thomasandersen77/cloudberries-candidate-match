package no.cloudberries.candidatematch.matches.event

import mu.KotlinLogging
import no.cloudberries.candidatematch.matches.service.ProjectMatchingService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Event listener for project request events.
 * 
 * Handles automatic triggering of consultant matching when project requests are uploaded.
 * Uses async processing to avoid blocking the upload process.
 */
@Component
class ProjectRequestEventListener(
    private val projectMatchingService: ProjectMatchingService
) {
    
    private val logger = KotlinLogging.logger { }
    
    /**
     * Handles project request uploaded events by triggering automatic matching.
     * 
     * This method is called asynchronously to ensure that the project request upload
     * process is not blocked by the matching computation.
     */
    @EventListener
    @Async
    fun handleProjectRequestUploaded(event: ProjectRequestUploadedEvent) {
        logger.info { "Handling project request uploaded event: $event" }
        
        try {
            projectMatchingService.onProjectRequestUploaded(event.projectRequestId)
            logger.info { "Successfully triggered automatic matching for project ${event.projectRequestId}" }
        } catch (e: Exception) {
            logger.error(e) { 
                "Failed to trigger automatic matching for project ${event.projectRequestId}: ${e.message}" 
            }
        }
    }
}