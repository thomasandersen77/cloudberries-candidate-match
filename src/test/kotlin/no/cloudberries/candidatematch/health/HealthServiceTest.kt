package no.cloudberries.candidatematch.health

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Query
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseHttpClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HealthServiceTest {

    // Mocks for alle avhengigheter i HealthService
    private lateinit var flowcaseHttpClient: FlowcaseHttpClient
    private lateinit var aiHealthChecker1: AIHealthChecker
    private lateinit var aiHealthChecker2: AIHealthChecker
    private lateinit var entityManager: EntityManager
    private lateinit var entityManagerFactory: EntityManagerFactory
    private lateinit var query: Query

    // Instans av klassen som testes
    private lateinit var healthService: HealthService

    @BeforeEach
    fun setUp() {
        // Initialiserer mocks før hver test
        flowcaseHttpClient = mockk()
        aiHealthChecker1 = mockk()
        aiHealthChecker2 = mockk()
        entityManager = mockk()
        entityManagerFactory = mockk()
        query = mockk()

        // Setter opp mock-hierarkiet for databasetesten
        every { entityManager.entityManagerFactory } returns entityManagerFactory
        every { entityManagerFactory.createEntityManager() } returns entityManager
        every { entityManager.createNativeQuery(any()) } returns query

        // Oppretter en ny instans av HealthService med de mockede avhengighetene
        healthService = HealthService(
            flowcaseHttpClient = flowcaseHttpClient,
            aiHealthCheckers = listOf(aiHealthChecker1, aiHealthChecker2),
            entityManager = entityManager
        )
    }

    @Test
    fun `isDatabaseHealthy returnerer true når database er tilgjengelig`() {
        // Arrange: Simulerer at databasekallet lykkes
        every { query.setHint("jakarta.persistence.query.timeout", 5000).singleResult } returns 1

        // Act: Kaller metoden
        val isHealthy = healthService.isDatabaseHealthy()

        // Assert: Forventer true
        assertTrue(isHealthy)
    }

    @Test
    fun `isDatabaseHealthy returnerer false når databasekall feiler`() {
        // Arrange: Simulerer at databasekallet kaster en exception
        every { query.setHint("jakarta.persistence.query.timeout", 5000).singleResult } throws RuntimeException("Database connection error")

        // Act: Kaller metoden
        val isHealthy = healthService.isDatabaseHealthy()

        // Assert: Forventer false
        assertFalse(isHealthy)
    }

    @Test
    fun `isAIHealthy returnerer true når minst én AI-tjeneste er sunn`() {
        // Arrange: Én AI er sunn, den andre ikke
        every { aiHealthChecker1.isHealthy() } returns true
        every { aiHealthChecker2.isHealthy() } returns false

        // Act & Assert
        assertTrue(healthService.isAIHealthy())
    }

    @Test
    fun `isAIHealthy returnerer false når ingen AI-tjenester er sunne`() {
        // Arrange: Begge AI-tjenestene er nede
        every { aiHealthChecker1.isHealthy() } returns false
        every { aiHealthChecker2.isHealthy() } returns false

        // Act & Assert
        assertFalse(healthService.isAIHealthy())
    }

    @Test
    fun `areAIConfigured returnerer true når minst én AI-tjeneste er konfigurert`() {
        // Arrange: Én AI er konfigurert, den andre ikke
        every { aiHealthChecker1.isConfigured() } returns true
        every { aiHealthChecker2.isConfigured() } returns false

        // Act & Assert
        assertTrue(healthService.areAIConfigured())
    }

    @Test
    fun `areAIConfigured returnerer false når ingen AI-tjenester er konfigurert`() {
        // Arrange: Ingen av AI-tjenestene er konfigurert
        every { aiHealthChecker1.isConfigured() } returns false
        every { aiHealthChecker2.isConfigured() } returns false

        // Act & Assert
        assertFalse(healthService.areAIConfigured())
    }

    @Test
    fun `checkOverallHealth returnerer true når alle avhengigheter er sunne`() {
        // Arrange: Alle systemer er "go"
        every { flowcaseHttpClient.checkHealth() } returns true
        every { query.setHint("jakarta.persistence.query.timeout", 5000).singleResult } returns 1
        every { aiHealthChecker1.isConfigured() } returns true
        every { aiHealthChecker1.isHealthy() } returns true

        // Act & Assert
        assertTrue(healthService.checkOverallHealth())
    }

    @Test
    fun `checkOverallHealth returnerer false når databasen er nede`() {
        // Arrange: Databasen feiler, resten er ok
        every { flowcaseHttpClient.checkHealth() } returns true
        every { query.singleResult } throws RuntimeException("DB down")
        every { aiHealthChecker1.isConfigured() } returns true
        every { aiHealthChecker1.isHealthy() } returns true

        // Act & Assert
        assertFalse(healthService.checkOverallHealth())
    }

    @Test
    fun `checkOverallHealth returnerer false når Flowcase er nede`() {
        // Arrange: Flowcase feiler, resten er ok
        every { flowcaseHttpClient.checkHealth() } returns false
        every { query.singleResult } returns 1
        every { aiHealthChecker1.isConfigured() } returns true
        every { aiHealthChecker1.isHealthy() } returns true

        // Act & Assert
        assertFalse(healthService.checkOverallHealth())
        // Verifiserer at checkHealth() faktisk ble kalt på http-klienten
        verify(exactly = 1) { flowcaseHttpClient.checkHealth() }
    }

    @Test
    fun `checkOverallHealth returnerer false når Flowcase-sjekk kaster exception`() {
        // Arrange: Flowcase-kallet kaster en feil
        every { flowcaseHttpClient.checkHealth() } throws RuntimeException("Network issue")
        every { query.singleResult } returns 1
        every { aiHealthChecker1.isConfigured() } returns true
        every { aiHealthChecker1.isHealthy() } returns true

        // Act & Assert
        assertFalse(healthService.checkOverallHealth())
    }

    @Test
    fun `checkOverallHealth returnerer false når ingen AI er konfigurert`() {
        // Arrange: AI-tjenester er ikke konfigurert, resten er ok
        every { flowcaseHttpClient.checkHealth() } returns true
        every { query.singleResult } returns 1
        every { aiHealthChecker1.isConfigured() } returns false
        every { aiHealthChecker2.isConfigured() } returns false
        every { aiHealthChecker1.isHealthy() } returns true // Selv om den er sunn, er den ikke konfigurert

        // Act & Assert
        assertFalse(healthService.checkOverallHealth())
    }

    @Test
    fun `checkOverallHealth returnerer false når ingen AI er operativ`() {
        // Arrange: AI-tjenester er konfigurert, men ikke sunne
        every { flowcaseHttpClient.checkHealth() } returns true
        every { query.singleResult } returns 1
        every { aiHealthChecker1.isConfigured() } returns true
        every { aiHealthChecker2.isConfigured() } returns true
        every { aiHealthChecker1.isHealthy() } returns false
        every { aiHealthChecker2.isHealthy() } returns false

        // Act & Assert
        assertFalse(healthService.checkOverallHealth())
    }
}