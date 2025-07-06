package no.cloudberries.candidatematch.service

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
