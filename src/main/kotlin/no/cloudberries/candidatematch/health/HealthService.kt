package no.cloudberries.candidatematch.health

import jakarta.persistence.EntityManager
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.integration.gemini.GeminiConfig
import no.cloudberries.candidatematch.integration.gemini.GeminiHttpClient
import no.cloudberries.candidatematch.integration.openai.OpenAIConfig
import no.cloudberries.candidatematch.integration.openai.OpenAIHttpClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HealthService(
    private val flowcaseHttpClient: FlowcaseHttpClient,
    private val openAIConfig: OpenAIConfig,
    private val geminiConfig: GeminiConfig,
    private val openAIHttpClient: OpenAIHttpClient,
    private val geminiHttpClient: GeminiHttpClient,
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
     * Sjekker den overordnede helsen til applikasjonens eksterne avhengigheter.
     * @return `true` hvis alle kritiske tjenester er sunne, ellers `false`.
     */
    fun areServicesHealthy(): Boolean {
        val flowcaseHealthy = checkFlowcaseHealth()
        val isAIConfigured  = isGenAiConfigured()
        val isGenAiHealthy = checkGenAiHealth()

        if (!isGenAiHealthy) logger.error("GenAI health check failed.")
        if (!flowcaseHealthy) logger.error("Flowcase health check failed.")
        if (!isAIConfigured) logger.error("AI services configuration check failed.")

        return flowcaseHealthy && isAIConfigured && isGenAiHealthy
    }

    /**
     * Sjekker helsen til Flowcase-integrasjonen ved å kalle et lettvektig endepunkt.
     */
    private fun checkFlowcaseHealth(): Boolean {
        return try {
            val isHealthy = flowcaseHttpClient.checkHealth()
            if (isHealthy) {
                logger.info("Health Check: Flowcase connection is OK.")
            } else {
                logger.warn("Health Check: Flowcase returned a non-successful status.")
            }
            isHealthy
        } catch (e: Exception) {
            logger.error("Health Check FAILED for Flowcase: ${e.message}")
            false
        }
    }

    /**
     * Sjekker helsen til AI-tjenestene (OpenAI/Gemini) ved å verifisere at API-nøkler er konfigurert.
     * Tjenesten anses som sunn hvis minst én av AI-leverandørene er konfigurert.
     */
    fun isGenAiConfigured(): Boolean {
        val isOpenAiConfigured = openAIConfig.apiKey.isNotBlank()
        val isGeminiConfigured = geminiConfig.apiKey.isNotBlank()

        if (isOpenAiConfigured) logger.info("Health Check: OpenAI is configured.")
        if (isGeminiConfigured) logger.info("Health Check: Gemini is configured.")

        val isHealthy = isOpenAiConfigured || isGeminiConfigured

        if (!isHealthy) {
            logger.error("Health Check FAILED for GenAI: Neither OpenAI nor Gemini API key is configured.")
        }
        return isHealthy
    }

    private fun checkGenAiHealth(): Boolean {
        // Prøver å koble til OpenAI
        val isOpenAiHealthy = try {
            openAIHttpClient.testConnection()
            logger.info("Health Check: OpenAI connection is OK.")
            true
        } catch (e: Exception) {
            logger.warn("Health Check for OpenAI failed: ${e.message}")
            false
        }

        // Prøver å koble til Gemini
        val isGeminiHealthy = try {
            geminiHttpClient.testConnection()
            logger.info("Health Check: Gemini connection is OK.")
            true
        } catch (e: Exception) {
            logger.warn("Health Check for Gemini failed: ${e.message}")
            false
        }

        // Tjenesten anses som sunn hvis minst én av AI-leverandørene fungerer
        val isOverallHealthy = isOpenAiHealthy || isGeminiHealthy

        if (!isOverallHealthy) {
            logger.error("Health Check FAILED for GenAI: Could not connect to any AI service.")
        }

        return isOverallHealthy
    }
}