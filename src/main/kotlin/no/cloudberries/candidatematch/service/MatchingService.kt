package no.cloudberries.candidatematch.service

import no.cloudberries.candidatematch.domain.candidate.ConsultantMatchedEvent
import no.cloudberries.candidatematch.domain.event.DomainEventPublisher
import no.cloudberries.candidatematch.repositories.ConsultantRepository
import org.springframework.stereotype.Service
import java.time.Instant
/*
interface MatchingService {
    fun matchConsultantToProject(
        consultantId: ConsultantId,
        projectId: ProjectId
    ): MatchResult
}

@Service
class MatchingServiceImpl(
    private val aiService: AIService,
    private val consultantRepository: ConsultantRepository,
    private val eventPublisher: DomainEventPublisher
) : MatchingService {
    
    override fun matchConsultantToProject(
        consultantId: ConsultantId,
        projectId: ProjectId
    ): MatchResult {
        // Your matching logic
        
        // Publish domain event
        eventPublisher.publish(
            ConsultantMatchedEvent(
                consultantId = consultantId,
                projectId = projectId,
                matchScore = result.score,
                occurredOn = Instant.now()
            )
        )
        
        return result
    }
}
*/
