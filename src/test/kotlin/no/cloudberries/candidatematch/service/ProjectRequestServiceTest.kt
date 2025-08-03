package no.cloudberries.candidatematch.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.cloudberries.candidatematch.domain.ProjectRequest
import no.cloudberries.candidatematch.domain.ProjectRequestId
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.repositories.ProjectRequestEntity
import no.cloudberries.candidatematch.repositories.ProjectRequestRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ProjectRequestServiceTest {

    // Mocker avhengigheter for å isolere tjenesten som testes
    private val projectRequestRepository: ProjectRequestRepository = mockk(relaxed = true)
    private val aiService: AIService = mockk(relaxed = true)

    // Initialiserer tjenesten med mockede avhengigheter
    private val projectRequestService = ProjectRequestService(projectRequestRepository, aiService)

    @Test
    fun `skal opprette prosjektforespørsel når svarfrist er før startdato`() {
        // Gitt gyldige datoer
        val startDate = LocalDate.of(2024, 9, 1)
        val endDate = LocalDate.of(2024, 12, 31)
        val responseDeadline = LocalDate.of(2024, 8, 15)
        
        // Mock repository save method to return the same object with an ID
        val savedRequestSlot = slot<ProjectRequestEntity>()
        every { projectRequestRepository.save(capture(savedRequestSlot)) } answers {
            savedRequestSlot.captured.copy(id = 1L)
        }

        // Når createProjectRequest kalles
        val request = projectRequestService.createProjectRequest(
            customerName = "Testkunde AS",
            requiredSkills = listOf(Skill.KOTLIN, Skill.BACKEND),
            startDate = startDate,
            endDate = endDate,
            responseDeadline = responseDeadline
        )

        // Så skal forespørselen bli opprettet med riktige data
        assertEquals("Testkunde AS", request.customerName)
        assertEquals(2, request.requiredSkills.size)
        assertEquals(startDate, request.startDate)
        assertEquals(ProjectRequestId(1L), ProjectRequestId(request.id?.value))
        assertEquals(endDate, request.endDate)
        assertEquals(responseDeadline, request.responseDeadline)
        assertTrue(request.aISuggestions.isEmpty()) // Ingen AI-forslag i denne testen
    }

    @Test
    fun `skal kaste exception når svarfrist er etter startdato`() {
        // Gitt ugyldige datoer
        val startDate = LocalDate.of(2024, 9, 1)
        val endDate = LocalDate.of(2024, 12, 31)
        val responseDeadline = LocalDate.of(2024, 9, 2) // Ugyldig

        // Når createProjectRequest kalles, så forventer vi en exception
        val exception = assertThrows(IllegalArgumentException::class.java) {
            projectRequestService.createProjectRequest(
                customerName = "Testkunde AS",
                requiredSkills = listOf(Skill.KOTLIN),
                startDate = startDate,
                endDate = endDate,
                responseDeadline = responseDeadline
            )
        }

        // Og exception-meldingen skal være korrekt
        assertEquals("Svarfristen kan ikke være etter prosjektets startdato.", exception.message)
    }
}