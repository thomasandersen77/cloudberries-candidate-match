package no.cloudberries.candidatematch.service

import mu.KotlinLogging
import no.cloudberries.candidatematch.config.AISettings
import no.cloudberries.candidatematch.domain.AISuggestion
import no.cloudberries.candidatematch.domain.ProjectRequest
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.domain.toEntity
import no.cloudberries.candidatematch.infrastructure.entities.RequestStatus
import no.cloudberries.candidatematch.infrastructure.repositories.ProjectRequestRepository
import no.cloudberries.candidatematch.infrastructure.entities.toProjectRequest
import no.cloudberries.candidatematch.service.ai.AIService
import no.cloudberries.candidatematch.service.consultants.ConsultantWithCvService
import no.cloudberries.candidatematch.service.validation.InputValidationService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ProjectRequestService(
    private val projectRequestRepository: ProjectRequestRepository,
    private val aiService: AIService,
    private val consultantService: ConsultantWithCvService,
    private val validationService: InputValidationService,
    private val aiSettings: AISettings
) {
    
    private val logger = KotlinLogging.logger { }

    /**
     * Henter åpne prosjektforespørsler som har frist innenfor det gitte tidsrommet.
     */
    @Timed
    @Transactional(readOnly = true)
    fun findOpenRequestsDueWithin(from: LocalDateTime, to: LocalDateTime): List<ProjectRequest> {
        return projectRequestRepository.findOpenRequestsWithDeadlineBetween(from, to).map { it.toProjectRequest() }
    }
    
    /**
     * Henter alle prosjektforespørsler med paginering.
     */
    @Timed
    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable = PageRequest.of(0, 20)): Page<ProjectRequest> {
        return projectRequestRepository.findAll(pageable).map { it.toProjectRequest() }
    }
    
    /**
     * Henter en spesifikk prosjektforespørsel på ID.
     */
    @Timed
    @Transactional(readOnly = true)
    fun findById(id: Long): ProjectRequest? {
        return projectRequestRepository.findById(id).map { it.toProjectRequest() }.orElse(null)
    }

    /**
     * Oppretter en ny prosjektforespørsel, validerer den og trigger en automatisk
     * matching av konsulenter.
     *
     * @param customerName Navn på kunden.
     * @param requiredSkills Liste over påkrevde ferdigheter.
     * @param startDate Ønsket startdato for prosjektet.
     * @param endDate Ønsket sluttdato for prosjektet.
     * @param responseDeadline Frist for å svare på forespørselen.
     * @return Den opprettede prosjektforespørselen.
     * @throws IllegalArgumentException hvis svarfristen er etter startdatoen.
     */
    @Timed
    @Transactional
    fun createProjectRequest(
        customerName: String,
        requiredSkills: List<Skill>,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        responseDeadline: LocalDateTime,
        status: RequestStatus,
        requestDescription: String,
        responsibleSalespersonEmail: String,
    ): ProjectRequest {
        // Input validation and sanitization
        val sanitizedCustomerName = validationService.validateCustomerName(customerName)
        val sanitizedDescription = validationService.sanitizeForAIPrompt(requestDescription)
        val sanitizedEmail = validationService.validateEmail(responsibleSalespersonEmail)

        val projectRequest = ProjectRequest(
            customerName = sanitizedCustomerName,
            requiredSkills = requiredSkills,
            startDate = startDate,
            endDate = endDate,
            responseDeadline = responseDeadline,
            status = status,
            requestDescription = sanitizedDescription,
            responsibleSalespersonEmail = sanitizedEmail,
        )

        // Save the request
        val savedRequest = projectRequestRepository.save(projectRequest.toEntity())
        val savedProjectRequest = savedRequest.toProjectRequest()
        
        logger.info { "Created project request ${savedRequest.id} for customer $sanitizedCustomerName" }

        // Trigger AI-powered consultant matching if enabled
        if (aiSettings.enabled) {
            try {
                val suggestions = findMatchingConsultants(savedProjectRequest)
                // Update with suggestions if any were found
                if (suggestions.isNotEmpty()) {
                    val updatedRequest = savedProjectRequest.copy(aISuggestions = suggestions)
                    projectRequestRepository.save(updatedRequest.toEntity())
                    logger.info { "Generated ${suggestions.size} AI suggestions for project request ${savedRequest.id}" }
                    return updatedRequest
                }
            } catch (e: Exception) {
                logger.warn(e) { "AI matching failed for project request ${savedRequest.id}, proceeding without suggestions" }
            }
        }

        return savedProjectRequest
    }

    /**
     * Finner passende konsulenter ved hjelp av AI-analyse.
     * Implementerer fallback-strategi ved feil.
     */
    @Timed
    fun findMatchingConsultants(request: ProjectRequest): List<AISuggestion> {
        if (!aiSettings.enabled) {
            logger.debug { "AI matching disabled, returning empty suggestions" }
            return emptyList()
        }
        
        try {
            logger.info { "Starting AI matching for project request ${request.id?.value}" }
            
            // Get top consultants to analyze
            val consultants = consultantService.getTopConsultantsBySkills(
                skills = request.getTopSkills().map { it.name },
                limit = 20 // Analyze top 20 candidates
            )
            
            val suggestions = mutableListOf<AISuggestion>()
            val requestPrompt = buildProjectRequestPrompt(request)
            
            for (consultant in consultants) {
                try {
                    val cvText = "Consultant CV for ${consultant.name}"
                    if (cvText.isNotBlank()) {
                        val matchResponse = aiService.matchCandidate(
                            aiProvider = aiSettings.provider,
                            cv = cvText,
                            request = requestPrompt,
                            consultantName = consultant.name
                        )
                        
                        val suggestion = AISuggestion(
                            consultantName = consultant.name,
                            userId = consultant.userId,
                            cvId = consultant.cvId ?: "",
                            matchScore = matchResponse.totalScore.toDoubleOrNull() ?: 70.0,
                            justification = matchResponse.summary,
                            skills = consultant.skills,
                            projectRequest = request
                        )
                        
                        if (suggestion.scoreIsValid() && suggestion.hasAcceptableScore()) {
                            suggestions.add(suggestion)
                        }
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to analyze consultant ${consultant.name}" }
                }
            }
            
            // Sort by score descending and limit results
            val sortedSuggestions = suggestions
                .sortedByDescending { it.matchScore }
                .take(10)
                
            logger.info { "Generated ${sortedSuggestions.size} valid suggestions from ${consultants.size} consultants" }
            return sortedSuggestions
            
        } catch (e: Exception) {
            logger.error(e) { "AI matching completely failed for project request ${request.id?.value}" }
            
            // Fallback: return rule-based suggestions
            return if (aiSettings.fallbackEnabled) {
                generateFallbackSuggestions(request)
            } else {
                emptyList()
            }
        }
    }
    
    /**
     * Lukker en prosjektforespørsel.
     */
    @Timed
    @Transactional
    fun closeProjectRequest(id: Long): ProjectRequest? {
        val request = findById(id) ?: return null
        request.closeRequest()
        val updated = projectRequestRepository.save(request.toEntity())
        logger.info { "Closed project request $id" }
        return updated.toProjectRequest()
    }
    
    /**
     * Trigger ny AI-analyse for eksisterende prosjektforespørsel.
     */
    @Timed
    @Transactional
    fun analyzeProjectRequest(id: Long): ProjectRequest? {
        val request = findById(id) ?: return null
        
        if (!aiSettings.enabled) {
            logger.warn { "AI analysis requested for project $id but AI is disabled" }
            return request
        }
        
        try {
            val suggestions = findMatchingConsultants(request)
            val updatedRequest = request.copy(aISuggestions = suggestions)
            val saved = projectRequestRepository.save(updatedRequest.toEntity())
            logger.info { "Re-analyzed project request $id with ${suggestions.size} new suggestions" }
            return saved.toProjectRequest()
        } catch (e: Exception) {
            logger.error(e) { "Failed to re-analyze project request $id" }
            return request
        }
    }
    
    private fun buildProjectRequestPrompt(request: ProjectRequest): String {
        val sanitizedDescription = validationService.sanitizeForAIPrompt(request.requestDescription)
        
        return """
            Project: ${request.customerName}
            Description: $sanitizedDescription
            Required Skills: ${request.requiredSkills.joinToString(", ") { it.name }}
            Duration: ${request.startDate} to ${request.endDate}
            Response Deadline: ${request.responseDeadline}
        """.trimIndent()
    }
    
    private fun generateFallbackSuggestions(request: ProjectRequest): List<AISuggestion> {
        logger.info { "Generating fallback suggestions for project ${request.id?.value}" }
        
        try {
            // Simple rule-based matching as fallback
            val consultants = consultantService.getTopConsultantsBySkills(
                skills = request.getTopSkills().map { it.name },
                limit = 5
            )
            
            return consultants.mapIndexed { index, consultant ->
                AISuggestion(
                    consultantName = consultant.name,
                    userId = consultant.userId,
                    cvId = consultant.cvId ?: "",
                    matchScore = 70.0 - (index * 5), // Decreasing scores
                    justification = "Fallback match based on skill overlap: ${consultant.skills.joinToString(", ")}",
                    skills = consultant.skills,
                    projectRequest = request
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Even fallback suggestions failed" }
            return emptyList()
        }
    }
}

