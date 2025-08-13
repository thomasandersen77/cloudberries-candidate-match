package no.cloudberries.candidatematch.controllers.health

import no.cloudberries.candidatematch.health.HealthService
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/health")
class HealthController(
    val healthService: HealthService
) {


    @GetMapping
    fun healthCheck(): Health {
        val databaseHealthy = healthService.isDatabaseHealthy()
        val servicesHealthy = healthService.checkOverallHealth()
        val genAIHealthy = healthService.isAIHealthy()

        val status = if (databaseHealthy && servicesHealthy && genAIHealthy) Status.UP else Status.DOWN

        return Health.status(status)
            .withDetail("database", databaseHealthy)
            .withDetail("services", servicesHealthy)
            .withDetail("genAI", genAIHealthy)
            .build()
    }
}