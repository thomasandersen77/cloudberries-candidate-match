package no.cloudberries.candidatematch.domain.candidate

import no.cloudberries.candidatematch.domain.event.DomainEvent
import java.time.Instant

data class ConsultantMatchedEvent(
    val consultantName: String,
    val matchScore: String,
    val matchSummary: String,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent