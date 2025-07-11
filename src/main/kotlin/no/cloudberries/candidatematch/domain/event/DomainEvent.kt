package no.cloudberries.candidatematch.domain.event

import no.cloudberries.candidatematch.domain.candidate.ConsultantMatchedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant


interface DomainEvent {
    val occurredOn: Instant
}

interface DomainEventPublisher {
    fun publish(event: ConsultantMatchedEvent)
    fun publishAll(events: Collection<DomainEvent>)
}

@Component
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : DomainEventPublisher {

    override fun publish(event: ConsultantMatchedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publishAll(events: Collection<DomainEvent>) {
        //events.forEach { publish(it) }
    }
}