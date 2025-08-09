package no.cloudberries.candidatematch.service // FIKS: Korrekt pakkenavn

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.PersistenceException
import jakarta.persistence.Query
import no.cloudberries.candidatematch.health.HealthService
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.integration.gemini.GeminiConfig
import no.cloudberries.candidatematch.integration.gemini.GeminiHttpClient
import no.cloudberries.candidatematch.integration.openai.OpenAIConfig
import no.cloudberries.candidatematch.integration.openai.OpenAIHttpClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import io.mockk.MockKAnnotations
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseConfig
import org.junit.jupiter.api.BeforeEach

@ExtendWith(MockKExtension::class)
class HealthServiceTest {

    // --- Mocks for alle avhengigheter ---
    @MockK
    private lateinit var flowcaseHttpClient: FlowcaseHttpClient

    // FIKS: Manglende Mocks lagt til
    @MockK
    private lateinit var openAIHttpClient: OpenAIHttpClient

    @MockK
    private lateinit var geminiHttpClient: GeminiHttpClient

    @MockK
    private lateinit var entityManager: EntityManager

    @MockK
    private lateinit var openAIConfig: OpenAIConfig

    @MockK
    private lateinit var geminiConfig: GeminiConfig

    // Injiserer de overnevnte mocks inn i HealthService
    @InjectMockKs
    private lateinit var healthService: HealthService

    @MockK
    private lateinit var flowcaseConfig: FlowcaseConfig

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        // If needed, set up default behaviors for your mocks
        every { flowcaseConfig.apiKey } returns "test-key"
        every { flowcaseConfig.baseUrl } returns "http://test.url"
        every { openAIConfig.apiKey } returns "test-key"
        every { openAIConfig.model } returns "test-model"
        every { openAIConfig.assistantId } returns "test-assistant"
        every { geminiConfig.apiKey } returns "test-key"
        every { geminiConfig.model } returns "test-model"
    }


    // --- Tester for isDatabaseHealthy (uendret) ---
    @Test
    fun `isDatabaseHealthy returns true when database connection is ok`() {
        // Arrange
        mockDatabaseHealth(true)
        // Act
        val isHealthy = healthService.isDatabaseHealthy()
        // Assert
        assertTrue(isHealthy)
    }

    @Test
    fun `isDatabaseHealthy returns false when database connection fails`() {
        // Arrange
        mockDatabaseHealth(false)
        // Act
        val isHealthy = healthService.isDatabaseHealthy()
        // Assert
        assertFalse(isHealthy)
    }

    // --- Refaktorerte tester for areServicesHealthy ---

    @Test
    fun `areServicesHealthy returns true when all dependencies are healthy`() {
        // Arrange
        mockDatabaseHealth(true)
        every { flowcaseHttpClient.checkHealth() } returns true
        every { openAIHttpClient.testConnection() } returns Unit // Suksess kaster ikke exception
        every { geminiHttpClient.testConnection() } returns Unit // Suksess kaster ikke exception

        // Act
        val isHealthy = healthService.areServicesHealthy()

        // Assert
        assertTrue(isHealthy)
    }

    @Test
    fun `areServicesHealthy returns false when Flowcase is unhealthy`() {
        // Arrange
        mockDatabaseHealth(true)
        every { flowcaseHttpClient.checkHealth() } returns false // Simulerer usunn tjeneste
        mockHealthyAIClients()

        // Act
        val isHealthy = healthService.areServicesHealthy()

        // Assert
        assertFalse(isHealthy)
    }

    @Test
    fun `areServicesHealthy returns false when no AI services are healthy`() {
        // Arrange
        mockDatabaseHealth(true)
        every { flowcaseHttpClient.checkHealth() } returns true
        every { openAIHttpClient.testConnection() } throws RuntimeException("AI down")
        every { geminiHttpClient.testConnection() } throws RuntimeException("AI down")

        // Act
        val isHealthy = healthService.areServicesHealthy()

        // Assert
        assertFalse(isHealthy)
    }

    @Test
    fun `areServicesHealthy returns true when only one AI service is healthy`() {
        // Arrange
        mockDatabaseHealth(true)
        every { flowcaseHttpClient.checkHealth() } returns true
        every { openAIHttpClient.testConnection() } returns Unit // OpenAI er OK
        every { geminiHttpClient.testConnection() } throws RuntimeException("Gemini down")

        // Act
        val isHealthy = healthService.areServicesHealthy()

        // Assert
        assertTrue(isHealthy, "Should be healthy if at least one AI provider is responsive")
    }

    @Test
    fun `areServicesHealthy returns false when all dependencies are unhealthy`() {
        // Arrange
        mockDatabaseHealth(false)
        every { flowcaseHttpClient.checkHealth() } returns false
        every { openAIHttpClient.testConnection() } throws RuntimeException("AI down")
        every { geminiHttpClient.testConnection() } throws RuntimeException("AI down")

        // Act
        val isHealthy = healthService.areServicesHealthy()

        // Assert
        assertFalse(isHealthy)
    }


    // --- Hjelpefunksjoner for renere tester ---

    private fun mockDatabaseHealth(isHealthy: Boolean) {
        val mockEmFactory = mockk<EntityManagerFactory>()
        val mockEm = mockk<EntityManager>()
        val mockQuery = mockk<Query>()

        every { entityManager.entityManagerFactory } returns mockEmFactory
        every { mockEmFactory.createEntityManager() } returns mockEm
        every { mockEm.createNativeQuery("SELECT 1") } returns mockQuery

        if (isHealthy) {
            every { mockQuery.singleResult } returns 1
        } else {
            every { mockQuery.singleResult } throws PersistenceException("DB is down")
        }
    }

    private fun mockHealthyAIClients() {
        every { openAIHttpClient.testConnection() } returns Unit
        every { geminiHttpClient.testConnection() } returns Unit
    }
}