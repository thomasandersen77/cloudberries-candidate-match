package no.cloudberries.candidatematch.domain.customer

import no.cloudberries.candidatematch.domain.event.DomainEvent
import java.time.Instant

class ProjectExistsEvent(
    val id: String,
    override val occurredOn: Instant = Instant.now()
): DomainEvent