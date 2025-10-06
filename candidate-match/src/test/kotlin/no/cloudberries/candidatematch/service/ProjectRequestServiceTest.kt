package no.cloudberries.candidatematch.service

import io.mockk.*
import no.cloudberries.candidatematch.config.AISettings
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.infrastructure.entities.ProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.entities.RequestStatus
import no.cloudberries.candidatematch.infrastructure.repositories.ProjectRequestRepository
import no.cloudberries.candidatematch.service.ai.AIService
import no.cloudberries.candidatematch.service.consultants.ConsultantWithCvService
import no.cloudberries.candidatematch.service.validation.InputValidationService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class ProjectRequestServiceTest {

    private val projectRequestRepository = mockk<ProjectRequestRepository>()
    private val aiService = mockk<AIService>()
    private val consultantService = mockk<ConsultantWithCvService>()
    private val validationService = mockk<InputValidationService>(relaxed = true)
    private val aiSettings = AISettings(
        enabled = true,
        timeout = Duration.ofSeconds(30),
        provider = AIProvider.OPENAI,
        fallbackEnabled = true
    )

    private lateinit var projectRequestService: ProjectRequestService

    @BeforeEach
    fun setup() {
        clearAllMocks()
        projectRequestService = ProjectRequestService(
            projectRequestRepository,
            aiService,
            consultantService,
            validationService,
            aiSettings
        )
    }

    @Test
    fun `createProjectRequest should validate input and save request`() {
        // Given
        val customerName = "Test Customer"
        val skills = listOf(no.cloudberries.candidatematch.domain.candidate.Skill.of("KOTLIN"), no.cloudberries.candidatematch.domain.candidate.Skill.of("JAVA"))
        val startDate = LocalDateTime.now().plusDays(30)
        val endDate = LocalDateTime.now().plusDays(60)
        val responseDeadline = LocalDateTime.now().plusDays(20)
        val description = "Test project description"
        val email = "test@example.com"

        val sanitizedName = "Sanitized Customer"
        val sanitizedDescription = "Sanitized description"
        val sanitizedEmail = "test@example.com"

        every { validationService.validateCustomerName(customerName) } returns sanitizedName
        every { validationService.sanitizeForAIPrompt(description) } returns sanitizedDescription
        every { validationService.validateEmail(email) } returns sanitizedEmail

        val savedEntity = ProjectRequestEntity(
            id = 1L,
            customerName = sanitizedName,
            requiredSkills = skills,
            startDate = startDate,
            endDate = endDate,
            responseDeadline = responseDeadline,
            requestDescription = sanitizedDescription,
            responsibleSalespersonEmail = sanitizedEmail,
            status = RequestStatus.OPEN
        )

        every { projectRequestRepository.save(any<ProjectRequestEntity>()) } returns savedEntity
        every { consultantService.getTopConsultantsBySkills(any(), any()) } returns emptyList()

        // When
        val result = projectRequestService.createProjectRequest(
            customerName = customerName,
            requiredSkills = skills,
            startDate = startDate,
            endDate = endDate,
            responseDeadline = responseDeadline,
            status = RequestStatus.OPEN,
            requestDescription = description,
            responsibleSalespersonEmail = email
        )

        // Then
        assertNotNull(result)
        assertEquals(1L, result.id?.value)
        assertEquals(sanitizedName, result.customerName)
        assertEquals(sanitizedDescription, result.requestDescription)
        assertEquals(sanitizedEmail, result.responsibleSalespersonEmail)

        verify { validationService.validateCustomerName(customerName) }
        verify { validationService.sanitizeForAIPrompt(description) }
        verify { validationService.validateEmail(email) }
        verify { projectRequestRepository.save(any<ProjectRequestEntity>()) }
    }

    // TODO: Fix this test - currently has compilation issues with mock setup
    /*
    @Test
    fun `createProjectRequest should trigger AI matching when enabled`() {
        // Given
        val customerName = "Test Customer"
        val skills = listOf(no.cloudberries.candidatematch.domain.candidate.Skill.of("KOTLIN"))
        val startDate = LocalDateTime.now().plusDays(30)
        val endDate = LocalDateTime.now().plusDays(60)
        val responseDeadline = LocalDateTime.now().plusDays(20)
        val description = "Test project"
        val email = "test@example.com"

        every { validationService.validateCustomerName(any()) } returns customerName
        every { validationService.sanitizeForAIPrompt(any()) } returns description
        every { validationService.validateEmail(any()) } returns email

        val savedEntity = ProjectRequestEntity(
            id = 1L,
            customerName = customerName,
            requiredSkills = skills,
            startDate = startDate,
            endDate = endDate,
            responseDeadline = responseDeadline,
            requestDescription = description,
            responsibleSalespersonEmail = email,
            status = RequestStatus.OPEN
        )

        every { projectRequestRepository.save(any<ProjectRequestEntity>()) } returns savedEntity

        val mockConsultant = mockk<no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto>() {
            every { name } returns "John Doe"
            every { userId } returns "user123"
            every { cvId } returns "cv123"
            every { skills } returns listOf("KOTLIN")
            every { cvs } returns emptyList()
        }

        every { consultantService.getTopConsultantsBySkills(any(), any()) } returns listOf(mockConsultant)

        val matchResponse = CandidateMatchResponse(
            totalScore = "85",
            summary = "Good match for Kotlin skills"
        )
        every { aiService.matchCandidate(any(), any(), any(), any()) } returns matchResponse

        // When
        val result = projectRequestService.createProjectRequest(
            customerName = customerName,
            requiredSkills = skills,
            startDate = startDate,
            endDate = endDate,
            responseDeadline = responseDeadline,
            status = RequestStatus.OPEN,
            requestDescription = description,
            responsibleSalespersonEmail = email
        )

        // Then
        assertEquals(1, result.aISuggestions.size)
        val suggestion = result.aISuggestions.first()
        assertEquals("John Doe", suggestion.consultantName)
        assertEquals(85.0, suggestion.matchScore)
        assertEquals("Good match for Kotlin skills", suggestion.justification)

        verify { consultantService.getTopConsultantsBySkills(listOf("KOTLIN"), 20) }
        verify { aiService.matchCandidate(aiSettings.provider, "Java developer ", any(), "John Doe") }
    }
    */

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

        every { validationService.validateCustomerName("Testkunde AS") } returns "Testkunde AS"
        every { validationService.sanitizeForAIPrompt("Test request") } returns "Test request"
        every { validationService.validateEmail("pc@cloudberries.no") } returns "pc@cloudberries.no"
        every { consultantService.getTopConsultantsBySkills(any(), any()) } returns emptyList()

        val savedRequestSlot = slot<ProjectRequestEntity>()
        every { projectRequestRepository.save(capture(savedRequestSlot)) } answers {
            savedRequestSlot.captured.copy(id = 1L)
        }

        val request = projectRequestService.createProjectRequest(
            customerName = "Testkunde AS",
            requiredSkills = listOf(no.cloudberries.candidatematch.domain.candidate.Skill.of("Kotlin")),
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

        every { validationService.validateCustomerName("Testkunde AS") } returns "Testkunde AS"
        every { validationService.sanitizeForAIPrompt("Test request") } returns "Test request"
        every { validationService.validateEmail("pc@cloudberries.no") } returns "pc@cloudberries.no"
        every { consultantService.getTopConsultantsBySkills(any(), any()) } returns emptyList()

        every { projectRequestRepository.save(any<ProjectRequestEntity>()) } answers {
            firstArg<ProjectRequestEntity>().copy(id = 1L)
        }

        val request = projectRequestService.createProjectRequest(
            customerName = "Testkunde AS",
            requiredSkills = listOf(no.cloudberries.candidatematch.domain.candidate.Skill.of("Kotlin")),
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
        val deadlineIn47Hours = now.minusHours(47)
        val expectedRequest = ProjectRequestEntity(
            id = 1L,
            customerName = "Kunde AS",
            requestDescription = "Trenger en utvikler",
            status = RequestStatus.OPEN,
            responsibleSalespersonEmail = "selger@cloudberries.no",
            requiredSkills = listOf(Skill.of("Kotlin"), Skill.of("Backend")),
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