package no.cloudberries.candidatematch.service

import no.cloudberries.candidatematch.domain.AISuggestion
import no.cloudberries.candidatematch.domain.ProjectRequest
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.domain.toEntity
import no.cloudberries.candidatematch.repositories.ProjectRequestRepository
import no.cloudberries.candidatematch.repositories.toProjectRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ProjectRequestService(
    private val projectRequestRepository: ProjectRequestRepository,
    private val aiService: AIService
) {

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
    fun createProjectRequest(
        customerName: String,
        requiredSkills: List<Skill>,
        startDate: LocalDate,
        endDate: LocalDate,
        responseDeadline: LocalDate
    ): ProjectRequest {
        // Validering i henhold til akseptansekriterium: frist for svar må være før oppstart
        if (responseDeadline.isAfter(startDate)) {
            throw IllegalArgumentException("Svarfristen kan ikke være etter prosjektets startdato.")
        }

            val projectRequest = ProjectRequest(
            customerName = customerName,
            requiredSkills = requiredSkills,
            startDate = startDate,
            endDate = endDate,
            responseDeadline = responseDeadline
        )

        // Lagre forespørselen (Dette ville vanligvis kalt et repository)
        val savedRequest = projectRequestRepository.save(projectRequest.toEntity()) // Forenklet for eksempelet

        // Trigger automatisk forslag av konsulenter (foreløpig skissert)
        // val suggestions = findMatchingConsultants(savedRequest)
        // savedRequest.aiSuggestions = suggestions

        return savedRequest.toProjectRequest()
    }

    // Skissert metode for fremtidig implementering av konsulent-matching
    fun findMatchingConsultants(request: ProjectRequest): List<AISuggestion> {
        // Her ville logikken for å hente konsulenter og kalle aiService ligget
        return emptyList()
    }
}

