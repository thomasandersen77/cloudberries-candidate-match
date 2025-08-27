package no.cloudberries.candidatematch.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.cloudberries.candidatematch.domain.ProjectRequestId
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.entities.ProjectRequestEntity
import no.cloudberries.candidatematch.entities.RequestStatus
import no.cloudberries.candidatematch.repositories.ProjectRequestRepository
import no.cloudberries.candidatematch.service.ai.AIService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ProjectRequestServiceTest {

    // Mocker avhengigheter for å isolere tjenesten som testes
    private val projectRequestRepository: ProjectRequestRepository = mockk(relaxed = true)
    private val aiService: AIService = mockk(relaxed = true)

    // Initialiserer tjenesten med mockede avhengigheter
    private val projectRequestService = ProjectRequestService(projectRequestRepository, aiService)

    @Test
    fun `skal opprette prosjektforespørsel når svarfrist er før startdato`() {
        // Gitt gyldige datoer
        val startDate = LocalDateTime.of(2024, 9, 1, 12, 0)
        val endDate = LocalDateTime.of(2024, 12, 31, 12, 0)
        val responseDeadline = LocalDateTime.of(2024, 8, 15, 12, 0)
        
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
            responseDeadline = responseDeadline,
            status = RequestStatus.OPEN,
            requestDescription = "Test request",
            responsibleSalespersonEmail = "pc@cloudberries.no"
        )

        // Så skal forespørselen bli opprettet med riktige data
        assertEquals("Testkunde AS", request.customerName)
        assertEquals(2, request.requiredSkills.size)
        assertEquals(startDate, request.startDate)
        assertEquals(ProjectRequestId(1L), request.id)
        assertEquals(endDate, request.endDate)
        assertEquals(responseDeadline, request.responseDeadline)
        assertTrue(request.aISuggestions.isEmpty())
    }

    @Test
    fun `skal kaste exception når svarfrist er etter startdato`() {
        // Gitt ugyldige datoer
        val startDate = LocalDateTime.of(2024, 9, 1, 12, 0)
        val endDate = LocalDateTime.of(2024, 12, 31, 12, 0)
        val responseDeadline = LocalDateTime.of(2024, 9, 2, 12, 0) // Ugyldig

        // Når createProjectRequest kalles, så forventer vi en exception
        val exception = assertThrows(IllegalArgumentException::class.java) {
            projectRequestService.createProjectRequest(
                customerName = "Testkunde AS",
                requiredSkills = listOf(Skill.KOTLIN),
                startDate = startDate,
                endDate = endDate,
                responseDeadline = responseDeadline,
                status = RequestStatus.OPEN,
                requestDescription = "Test request",
                responsibleSalespersonEmail = "pc@cloudberries.no"
            )
        }

        // Og exception-meldingen skal være korrekt
        assertEquals("Svarfristen kan ikke være etter prosjektets startdato.", exception.message)
    }

    @Test
    fun `skal lukke prosjektforespørsel`() {
        // Given
        val startDate = LocalDateTime.of(
            2024,
            9,
            1,
            12,
            0
        )
        val endDate = LocalDateTime.of(
            2024,
            12,
            31,
            12,
            0
        )
        val responseDeadline = LocalDateTime.of(
            2024,
            8,
            15,
            12,
            0
        )

        val savedRequestSlot = slot<ProjectRequestEntity>()
        every { projectRequestRepository.save(capture(savedRequestSlot)) } answers {
            savedRequestSlot.captured.copy(id = 1L)
        }

        val request = projectRequestService.createProjectRequest(
            customerName = "Testkunde AS",
            requiredSkills = listOf(Skill.KOTLIN),
            startDate = startDate,
            endDate = endDate,
            responseDeadline = responseDeadline,
            status = RequestStatus.OPEN,
            requestDescription = "Test request",
            responsibleSalespersonEmail = "pc@cloudberries.no"
        )

        // When
        request.closeRequest()

        // Then
        assertEquals(
            RequestStatus.CLOSED,
            request.status
        )
    }

    @Test
    fun `skal kaste exception når lukker allerede lukket forespørsel`() {
        // Given
        val startDate = LocalDateTime.of(
            2024,
            9,
            1,
            12,
            0
        )
        val endDate = LocalDateTime.of(
            2024,
            12,
            31,
            12,
            0
        )
        val responseDeadline = LocalDateTime.of(
            2024,
            8,
            15,
            12,
            0
        )

        val savedRequestSlot = slot<ProjectRequestEntity>()
        every { projectRequestRepository.save(capture(savedRequestSlot)) } answers {
            savedRequestSlot.captured.copy(id = 1L)
        }

        val request = projectRequestService.createProjectRequest(
            customerName = "Testkunde AS",
            requiredSkills = listOf(Skill.KOTLIN),
            startDate = startDate,
            endDate = endDate,
            responseDeadline = responseDeadline,
            status = RequestStatus.CLOSED,
            requestDescription = "Test request",
            responsibleSalespersonEmail = "pc@cloudberries.no"
        )

        // When/Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            request.closeRequest()
        }
        assertEquals(
            "Forespørselen er allerede lukket",
            exception.message
        )
    }

    @Test
    fun `skal finne åpne forespørsler som nærmer seg fristen`() {
        // Gitt
        val now = LocalDateTime.now()
        val deadlineIn47Hours = now.plusHours(47)
        val expectedRequest = ProjectRequestEntity(
            id = 1L,
            customerName = "Kunde AS",
            requestDescription = "Trenger en utvikler",
            status = RequestStatus.OPEN,
            responsibleSalespersonEmail = "selger@cloudberries.no",
            requiredSkills = listOf(Skill.KOTLIN, Skill.BACKEND),
            startDate = LocalDate.now().atStartOfDay(),
            endDate = LocalDate.now().plusDays(10).atStartOfDay(),
            responseDeadline = deadlineIn47Hours,
            aiSuggestionEntities = emptyList()

        )

        every { projectRequestRepository.findOpenRequestsWithDeadlineBetween(any(), any()) } returns listOf(expectedRequest)

        // Når
        val result = projectRequestService.findOpenRequestsDueWithin(now, now.plusHours(48))

        // Så
        assertEquals(1, result.size)
        assertEquals("Kunde AS", result.first().customerName)
        verify(exactly = 1) { projectRequestRepository.findOpenRequestsWithDeadlineBetween(now, now.plusHours(48)) }
    }
}