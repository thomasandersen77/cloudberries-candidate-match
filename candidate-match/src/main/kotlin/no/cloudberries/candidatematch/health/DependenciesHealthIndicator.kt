package no.cloudberries.candidatematch.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component("dependencies") // Navnet "dependencies" brukes i spring konfigurasjonen
class DependenciesHealthIndicator(
    private val healthService: HealthService
) : HealthIndicator {

    /**
     * Denne metoden kalles av Spring Actuator for å sjekke helsen.
     * Den bruker HealthService til å sjekke databasen og eksterne tjenester.
     */
    override fun health(): Health {
        return try {
            val databaseHealthy = healthService.isDatabaseHealthy()
            val servicesHealthy = healthService.checkOverallHealth() // denne sjekker Flowcase/AI

            if (databaseHealthy && servicesHealthy) {
                Health.up().withDetail("message", "All dependencies are healthy").build()
            } else {
                // Bygg en detaljert feilmelding
                val details = mutableMapOf<String, Any>()
                details["database"] = if (databaseHealthy) "UP" else "DOWN"
                details["external_services"] = if (servicesHealthy) "UP" else "DOWN"

                Health.down()
                    .withDetail("message", "One or more dependencies are unhealthy")
                    .withDetails(details)
                    .build()
            }
        } catch (e: Exception) {
            Health.down(e)
                .withDetail("error", e.message ?: "Unknown exception")
                .build()
        }
    }
}