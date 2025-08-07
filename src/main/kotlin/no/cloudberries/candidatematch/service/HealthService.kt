package no.cloudberries.candidatematch.service

import jakarta.persistence.EntityManager
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.integration.gemini.GeminiConfig
import no.cloudberries.candidatematch.integration.openai.OpenAIConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HealthService(
    private val flowcaseHttpClient: FlowcaseHttpClient,
    private val openAIConfig: OpenAIConfig,
    private val geminiConfig: GeminiConfig,
    private val entityManager: EntityManager
) {

    private val logger = LoggerFactory.getLogger(HealthService::class.java)

    fun isDatabaseHealthy(): Boolean = runCatching {
        entityManager.entityManagerFactory
            .createEntityManager()
            .createNativeQuery("SELECT 1")
            .singleResult != null
    }.getOrElse { false }

    /**
     * Sjekker den overordnede helsen til applikasjonens eksterne avhengigheter.
     * @return `true` hvis alle kritiske tjenester er sunne, ellers `false`.
     */
    fun isServiceHealthy(): Boolean {
        val flowcaseHealthy = checkFlowcaseHealth()
        val aiHealthy = checkGenAiHealth()
        val databaseHealthy = isDatabaseHealthy()

        if (!flowcaseHealthy) logger.error("Flowcase health check failed.")
        if (!aiHealthy) logger.error("AI services health check failed.")
        if (!databaseHealthy) logger.error("Database health check failed.")

        return flowcaseHealthy && aiHealthy && databaseHealthy
    }

    /**
     * Sjekker helsen til Flowcase-integrasjonen ved å utføre et test-kall.
     */
    private fun checkFlowcaseHealth(): Boolean {
        return try {
            // Vi antar at hvis vi får en respons uten exception, er tjenesten oppe.
            flowcaseHttpClient.fetchAllCvs()
            logger.info("Health Check: Flowcase connection is OK.")
            true
        } catch (e: Exception) {
            logger.error("Health Check FAILED for Flowcase: ${e.message}")
            false
        }
    }

    /**
     * Sjekker helsen til AI-tjenestene (OpenAI/Gemini) ved å verifisere at API-nøkler er konfigurert.
     * Tjenesten anses som sunn hvis minst én av AI-leverandørene er konfigurert.
     */
    fun checkGenAiHealth(): Boolean {
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
}