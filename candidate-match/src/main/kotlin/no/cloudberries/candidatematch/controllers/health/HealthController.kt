package no.cloudberries.candidatematch.controllers.health

import mu.KotlinLogging
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
    private val logger = KotlinLogging.logger { }

    @GetMapping
    @no.cloudberries.candidatematch.utils.Timed
    fun healthCheck(): Health {
        logger.info { "GET /api/health" }
        val status = if (healthService.checkOverallHealth()) Status.UP else Status.DOWN

        return Health.status(status)
            .withDetails(healthService.getHealthDetails()) // Hent detaljer fra service
            .build()
    }
}
