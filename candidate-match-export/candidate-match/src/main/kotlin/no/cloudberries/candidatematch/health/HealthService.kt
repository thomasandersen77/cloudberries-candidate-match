package no.cloudberries.candidatematch.health

import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service
class HealthService(
    private val flowcaseHttpClient: FlowcaseHttpClient,
    private val aiHealthCheckers: List<AIHealthChecker>,
    private val entityManager: EntityManager
) {

    private val logger = mu.KotlinLogging.logger { }
    private val cacheLock = ReentrantReadWriteLock()
    
    // Cache with 60-minute TTL
    private data class CachedHealth(
        val details: Map<String, Any>,
        val overallStatus: Boolean,
        val timestamp: Instant
    )
    
    @Volatile
    private var cachedHealth: CachedHealth? = null
    private val cacheTtlMinutes = 60L
    
    @PostConstruct
    fun initializeHealthCache() {
        logger.info { "Initializing health cache on startup..." }
        refreshHealthCache()
    }
    
    private fun isCacheValid(): Boolean {
        val cached = cachedHealth ?: return false
        val age = java.time.Duration.between(cached.timestamp, Instant.now())
        return age.toMinutes() < cacheTtlMinutes
    }
    
    private fun refreshHealthCache() {
        cacheLock.write {
            val details = computeHealthDetails()
            val overall = computeOverallHealth(details)
            cachedHealth = CachedHealth(details, overall, Instant.now())
            logger.info { "Health cache refreshed. Overall status: $overall" }
        }
    }

    // Private methods that actually perform the checks (expensive)
    @Timed
    private fun isDatabaseHealthyInternal(): Boolean = runCatching {
        entityManager
            .createNativeQuery("SELECT 1")
            .setHint("jakarta.persistence.query.timeout", 5000)
            .singleResult != null
    }.getOrElse { e ->
        logger.error { "Database health check failed: ${e.message}" }
        false
    }

    @Timed
    private fun checkFlowcaseHealthInternal(): Boolean =
        try {
            flowcaseHttpClient.checkHealth()
        } catch (e: Exception) {
            logger.error { "Health Check FAILED for Flowcase: ${e.message}" }
            false
        }

    private fun isAIHealthyInternal(): Boolean = aiHealthCheckers.any { it.isHealthy() }
    private fun areAIConfiguredInternal(): Boolean = aiHealthCheckers.any { it.isConfigured() }
    
    // Compute methods that perform actual checks
    private fun computeHealthDetails(): Map<String, Any> {
        return mapOf(
            "database" to isDatabaseHealthyInternal(),
            "flowcase" to checkFlowcaseHealthInternal(),
            "genAI_operational" to isAIHealthyInternal(),
            "genAI_configured" to areAIConfiguredInternal()
        )
    }
    
    private fun computeOverallHealth(details: Map<String, Any>): Boolean {
        val isDatabaseHealthy = details["database"] as? Boolean ?: false
        val isFlowcaseHealthy = details["flowcase"] as? Boolean ?: false
        val areAIConfigured = details["genAI_configured"] as? Boolean ?: false
        val isAIOperational = (details["genAI_operational"] as? Boolean ?: false) && areAIConfigured

        if (isDatabaseHealthy) {
            logger.info { "Database health check passed." }
        } else {
            logger.error { "Database health check failed." }
        }

        if (!areAIConfigured) {
            logger.error { "AI services configuration check failed." }
        } else {
            logger.info { "AI services configuration check passed." }
        }

        if (!isAIOperational) {
            logger.error { "GenAI health check failed. Neither service is operational." }
        } else {
            logger.info { "GenAI health check passed. Both services are operational." }
        }

        if (!isFlowcaseHealthy) {
            logger.error { "Flowcase health check failed." }
        } else {
            logger.info { "Flowcase health check passed." }
        }

        return isFlowcaseHealthy && isAIOperational && isDatabaseHealthy
    }


    /**
     * Get cached health details. Refreshes cache if expired.
     */
    fun getHealthDetails(): Map<String, Any> {
        return cacheLock.read {
            if (!isCacheValid()) {
                // Upgrade to write lock
                cacheLock.write {
                    // Double-check after acquiring write lock
                    if (!isCacheValid()) {
                        refreshHealthCache()
                    }
                }
            }
            cachedHealth?.details ?: emptyMap()
        }
    }

    /**
     * Check overall health status from cache. Refreshes cache if expired.
     * @return `true` if all critical services are healthy, else `false`.
     */
    fun checkOverallHealth(): Boolean {
        return cacheLock.read {
            if (!isCacheValid()) {
                // Upgrade to write lock
                cacheLock.write {
                    // Double-check after acquiring write lock
                    if (!isCacheValid()) {
                        refreshHealthCache()
                    }
                }
            }
            cachedHealth?.overallStatus ?: false
        }
    }
    
    /**
     * Check if database is healthy from cache.
     */
    fun isDatabaseHealthy(): Boolean {
        val details = getHealthDetails()
        return details["database"] as? Boolean ?: false
    }
    
    /**
     * Check if AI services are configured from cache.
     */
    fun areAIConfigured(): Boolean {
        val details = getHealthDetails()
        return details["genAI_configured"] as? Boolean ?: false
    }

}
