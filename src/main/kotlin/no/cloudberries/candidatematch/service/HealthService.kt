package no.cloudberries.candidatematch.service

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class HealthService(
    val entityManager: EntityManager
) {
    fun isDatabaseHealthy(): Boolean = runCatching {
        entityManager.entityManagerFactory
            .createEntityManager()
            .createNativeQuery("SELECT 1")
            .singleResult != null
    }.getOrElse { false }
    fun isServiceHealthy(): Boolean = true
}
