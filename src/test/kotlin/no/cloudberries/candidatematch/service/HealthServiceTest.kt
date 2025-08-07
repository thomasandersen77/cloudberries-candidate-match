package no.cloudberries.candidatematch.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.PersistenceException
import jakarta.persistence.Query
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseResumeResponse
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseUserSearchResponse
import no.cloudberries.candidatematch.integration.gemini.GeminiConfig
import no.cloudberries.candidatematch.integration.openai.OpenAIConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class HealthServiceTest {

    @MockK
    private lateinit var flowcaseHttpClient: FlowcaseHttpClient

    @MockK
    private lateinit var openAIConfig: OpenAIConfig

    @MockK
    private lateinit var geminiConfig: GeminiConfig

    @InjectMockKs
    private lateinit var healthService: HealthService

    @MockK
    private lateinit var entityManager: EntityManager // <-- LEGG TIL DENNE MOCKEN


    @Test
    fun `isDatabaseHealthy returns true when database connection is ok`() {
        // Arrange
        val mockEmFactory = mockk<EntityManagerFactory>()
        val mockEm = mockk<EntityManager>()
        val mockQuery = mockk<Query>()

        every { entityManager.entityManagerFactory } returns mockEmFactory
        every { mockEmFactory.createEntityManager() } returns mockEm
        every { mockEm.createNativeQuery("SELECT 1") } returns mockQuery
        every { mockQuery.singleResult } returns 1 // Returnerer et non-null objekt

        // Act
        val isHealthy = healthService.isDatabaseHealthy()

        // Assert
        assertTrue(isHealthy)
    }

    @Test
    fun `isDatabaseHealthy returns false when database connection fails`() {
        // Arrange
        val mockEmFactory = mockk<EntityManagerFactory>()
        val mockEm = mockk<EntityManager>()

        every { entityManager.entityManagerFactory } returns mockEmFactory
        every { mockEmFactory.createEntityManager() } returns mockEm
        // Simulerer at kallet feiler, f.eks. på grunn av feil i tilkobling
        every { mockEm.createNativeQuery("SELECT 1") } throws PersistenceException("Connection failed")

        // Act
        val isHealthy = healthService.isDatabaseHealthy()

        // Assert
        assertFalse(isHealthy)
    }

    @Test
    fun `isServiceHealthy returns true when all services are healthy`() {
        // Arrange
        every { healthService.isDatabaseHealthy() } returns true
        every { flowcaseHttpClient.fetchAllCvs() } returns FlowcaseResumeResponse(emptyList())
        every { openAIConfig.apiKey } returns "valid-openai-key"
        every { geminiConfig.apiKey } returns "valid-gemini-key"

        // Act
        val isHealthy = healthService.isServiceHealthy()

        // Assert
        assertTrue(isHealthy)
    }

    @Test
    fun `isServiceHealthy returns false when Flowcase is unhealthy`() {
        // Arrange
        every { flowcaseHttpClient.fetchAllCvs() } throws RuntimeException("Connection timed out")
        every { openAIConfig.apiKey } returns "valid-openai-key"
        every { geminiConfig.apiKey } returns "valid-gemini-key"

        // Act
        val isHealthy = healthService.isServiceHealthy()

        // Assert
        assertFalse(isHealthy)
    }

    @Test
    fun `isServiceHealthy returns false when no AI services are configured`() {
        // Arrange
        every { healthService.isDatabaseHealthy() } returns true
        every { flowcaseHttpClient.fetchAllCvs() } returns FlowcaseResumeResponse(emptyList())
        every { openAIConfig.apiKey } returns "" // Blank key
        every { geminiConfig.apiKey } returns " " // Blank key

        // Act
        val isHealthy = healthService.isServiceHealthy()

        // Assert
        assertFalse(isHealthy)
    }

    @Test
    fun `isServiceHealthy returns true when only one AI service is configured`() {
        // Arrange
        every { healthService.isDatabaseHealthy() } returns true
        every { flowcaseHttpClient.fetchAllCvs() } returns FlowcaseResumeResponse(emptyList())
        every { openAIConfig.apiKey } returns "valid-openai-key"
        every { geminiConfig.apiKey } returns "" // Gemini is not configured

        // Act
        val isHealthy = healthService.isServiceHealthy()

        // Assert
        assertTrue(isHealthy, "Service should be healthy if at least one AI provider is configured")
    }

    @Test
    fun `isServiceHealthy returns false when all services are unhealthy`() {
        // Arrange
        every { flowcaseHttpClient.fetchAllCvs() } throws RuntimeException("API down")
        every { openAIConfig.apiKey } returns ""
        every { geminiConfig.apiKey } returns ""

        // Act
        val isHealthy = healthService.isServiceHealthy()

        // Assert
        assertFalse(isHealthy)
    }
}