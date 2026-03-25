package no.cloudberries.candidatematch.service.notification

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.cloudberries.candidatematch.infrastructure.entities.ProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.entities.RequestStatus
import no.cloudberries.candidatematch.infrastructure.entities.toProjectRequest
import no.cloudberries.candidatematch.infrastructure.entities.AISuggestionEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class LoggingNotificationServiceTest {

    private val notificationService = LoggingNotificationService()

    // Hjelpeverktøy for å fange opp logg-meldinger
    private lateinit var listAppender: ListAppender<ILoggingEvent>
    private lateinit var logger: Logger

    @BeforeEach
    fun setup() {
        // Hent loggeren for klassen vi skal teste. Dette fungerer også for mu.KotlinLogging.
        logger = LoggerFactory.getLogger(LoggingNotificationService::class.java) as Logger

        // Opprett en appender som lagrer logs i en liste
        listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()

        // Koble vår appender til loggeren
        logger.addAppender(listAppender)
    }

    @AfterEach
    fun teardown() {
        // Rydd opp ved å fjerne appenderen for å holde testene isolerte
        logger.detachAppender(listAppender)
    }

    @Test
    fun `skal formatere varsel med alle forespørselsdetaljer og korrekt match-status`() {
        // GITT (Arrange)
        val responseDeadline = LocalDateTime.of(2026, 3, 25, 16, 0)
        val startDate = LocalDateTime.of(2026, 4, 1, 9, 0)
        val endDate = LocalDateTime.of(2026, 12, 31, 17, 0)

        val projectRequestEntity = ProjectRequestEntity(
            id = 101,
            customerName = "Testkunde AS",
            requestDescription = "Viktig oppdrag for en senior Kotlin-utvikler.",
            responseDeadline = responseDeadline,
            responsibleSalespersonEmail = "salg@cloudberries.no",
            status = RequestStatus.OPEN,
            startDate = startDate,
            endDate = endDate
        )

        // Create AI suggestions with proper constructor parameters
        val aiSuggestion1 = AISuggestionEntity(
            id = 1,
            consultantName = "John Doe",
            userId = "user123",
            cvId = "cv456",
            matchScore = 8.5,
            justification = "Strong match",
            projectRequest = projectRequestEntity
        )

        val aiSuggestion2 = AISuggestionEntity(
            id = 2,
            consultantName = "Jane Smith",
            userId = "user789",
            cvId = "cv101",
            matchScore = 9.0,
            justification = "Excellent match",
            projectRequest = projectRequestEntity
        )

        // Set the AI suggestions to the project request
        projectRequestEntity.aiSuggestionEntities = listOf(
            aiSuggestion1,
            aiSuggestion2
        )

        // Convert to domain object for the service call
        val projectRequest = projectRequestEntity.toProjectRequest()

        // NÅR (Act)
        notificationService.sendDeadlineReminder(projectRequest)

        // SÅ (Assert)
        val logsList = listAppender.list
        Assertions.assertTrue(
            logsList.isNotEmpty(),
            "Loggen skal ikke være tom"
        )

        val loggedMessage = logsList[0].formattedMessage

        // Verifiser at alle detaljer fra akseptansekriteriet er inkludert i loggmeldingen
        Assertions.assertTrue(
            loggedMessage.contains("salg@cloudberries.no"),
            "Mangler e-post til ansvarlig"
        )
        Assertions.assertTrue(
            loggedMessage.contains("Testkunde AS"),
            "Mangler kundenavn"
        )
        Assertions.assertTrue(
            loggedMessage.contains("2026-03-25T16:00"),
            "Mangler eller har feil format på frist"
        )
        Assertions.assertTrue(
            loggedMessage.contains("Viktig oppdrag for en senior Kotlin-utvikler."),
            "Mangler beskrivelse"
        )
       }
    }