package no.cloudberries.candidatematch.health

import jakarta.persistence.EntityManager
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseHttpClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HealthService(
    private val flowcaseHttpClient: FlowcaseHttpClient,
    private val aiHealthCheckers: List<AIHealthChecker>,
    private val entityManager: EntityManager
) {

    private val logger = LoggerFactory.getLogger(HealthService::class.java)

    fun isDatabaseHealthy(): Boolean = runCatching {
        entityManager.entityManagerFactory
            .createEntityManager()
            .createNativeQuery("SELECT 1")
            .singleResult != null
    }.getOrElse {
        logger.error("Database health check failed.")
        false
    }


    /**
     * Sjekker helsen til Flowcase-integrasjonen ved å kalle et lettvektig endepunkt.
     */
    fun checkFlowcaseHealth(): Boolean =
        try {
            flowcaseHttpClient.checkHealth()
        } catch (e: Exception) {
            logger.error("Health Check FAILED for Flowcase: ${e.message}")
            false
        }

    fun isAIHealthy(): Boolean = aiHealthCheckers.any { it.isHealthy() }
    fun areAIConfigured(): Boolean = aiHealthCheckers.any { it.isConfigured() }


    fun getHealthDetails(): Map<String, Any> {
        return mapOf(
            "database" to isDatabaseHealthy(),
            "flowcase" to checkFlowcaseHealth(),
            "genAI_operational" to isAIHealthy(), // Mer beskrivende navn
            "genAI_configured" to areAIConfigured()
        )
    }
    /**
     * Sjekker den overordnede helsen til applikasjonens eksterne avhengigheter.
     * @return `true` hvis alle kritiske tjenester er sunne, ellers `false`.
     */
    fun checkOverallHealth(): Boolean {
        val isFlowcaseHealthy = checkFlowcaseHealth()
        val areAIConfigured = areAIConfigured()
        val isAIOperational = isAIHealthy() && areAIConfigured
        val isDatabaseHealthy = isDatabaseHealthy()

        if (isDatabaseHealthy) {
            logger.info("Database health check passed.")
        } else {
            logger.error("Database health check failed.")
        }

        if (!areAIConfigured) {
            logger.error("AI services configuration check failed.")
        } else {
            logger.info("AI services configuration check passed.")
        }

        if (!isAIOperational) {
            logger.error("GenAI health check failed. Neither service is operational.")
        } else {
            logger.info("GenAI health check passed. Both services are operational.")
        }

        if (!isFlowcaseHealthy) {
            logger.error("Flowcase health check failed.")
        } else {
            logger.info("Flowcase health check passed.")
        }

        return isFlowcaseHealthy && isAIOperational && isDatabaseHealthy
    }

}