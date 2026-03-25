package no.cloudberries.candidatematch.health

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HealthServiceTest {

    private lateinit var flowcaseHttpClient: FlowcaseHttpClient
    private lateinit var entityManager: EntityManager
    private lateinit var query: Query
    private lateinit var healthService: HealthService

    @BeforeEach
    fun setUp() {
        flowcaseHttpClient = mockk()
        entityManager = mockk()
        query = mockk()

        every { entityManager.createNativeQuery(any()) } returns query

        healthService = HealthService(
            flowcaseHttpClient = flowcaseHttpClient,
            entityManager = entityManager
        )
    }

    @Test
    fun `isDatabaseHealthy returnerer true når database er tilgjengelig`() {
        every {
            query.setHint(
                "jakarta.persistence.query.timeout",
                5000
            ).singleResult
        } returns 1

        val isHealthy = healthService.isDatabaseHealthy()
        assertTrue(isHealthy)
    }

    @Test
    fun `isDatabaseHealthy returnerer false når databasekall feiler`() {
        every {
            query.setHint(
                "jakarta.persistence.query.timeout",
                5000
            ).singleResult
        } throws RuntimeException("Database connection error")

        val isHealthy = healthService.isDatabaseHealthy()
        assertFalse(isHealthy)
    }

    @Test
    fun `getHealthDetails returnerer riktig informasjon`() {
        every { flowcaseHttpClient.checkHealth() } returns true
        every { query.setHint("jakarta.persistence.query.timeout", 5000).singleResult } returns 1

        val details = healthService.getHealthDetails()

        assertTrue(details["database"] as Boolean)
        assertTrue(details["flowcase"] as Boolean)
        assertTrue(details["genAI_operational"] as Boolean)
        assertTrue(details["genAI_configured"] as Boolean)
    }

    @Test
    fun `checkOverallHealth returnerer true når alle avhengigheter er sunne`() {
        every { flowcaseHttpClient.checkHealth() } returns true
        every {
            query.setHint(
                "jakarta.persistence.query.timeout",
                5000
            ).singleResult
        } returns 1

        assertTrue(healthService.checkOverallHealth())
    }

    @Test
    fun `checkOverallHealth returnerer false når databasen er nede`() {
        every { flowcaseHttpClient.checkHealth() } returns true
        every { query.setHint("jakarta.persistence.query.timeout", 5000).singleResult } throws RuntimeException("DB down")

        assertFalse(healthService.checkOverallHealth())
    }

    @Test
    fun `checkOverallHealth returnerer false når Flowcase er nede`() {
        every { flowcaseHttpClient.checkHealth() } returns false
        every { query.setHint("jakarta.persistence.query.timeout", 5000).singleResult } returns 1

        assertFalse(healthService.checkOverallHealth())
        verify { flowcaseHttpClient.checkHealth() }
    }
}