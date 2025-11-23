package no.cloudberries.candidatematch.matches.event

import org.springframework.context.ApplicationEvent

/**
 * Domain event fired when a project request is uploaded.
 * 
 * This event triggers automatic consultant matching computation in the background.
 * Follows the Domain Event pattern to maintain loose coupling between components.
 */
class ProjectRequestUploadedEvent(
    source: Any,
    val projectRequestId: Long,
    val customerName: String
) : ApplicationEvent(source) {
    
    override fun toString(): String {
        return "ProjectRequestUploadedEvent(projectRequestId=$projectRequestId, customerName='$customerName')"
    }
}