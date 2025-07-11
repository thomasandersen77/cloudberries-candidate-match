package no.cloudberries.candidatematch.domain.candidate

import no.cloudberries.candidatematch.domain.event.DomainEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class DomainEventListener {
    
    @EventListener
    fun handleDomainEvent(event: DomainEvent) {
        when (event) {
            // Add other event types here
            is ConsultantMatchedEvent -> handleConsultantMatched(event)
        }
    }
    
    private fun handleConsultantMatched(event: ConsultantMatchedEvent) {
        // Handle the event
    }
}