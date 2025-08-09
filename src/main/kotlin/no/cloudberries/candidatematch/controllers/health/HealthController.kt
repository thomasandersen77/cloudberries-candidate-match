package no.cloudberries.candidatematch.controllers.health

import no.cloudberries.candidatematch.health.HealthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/health")
class HealthController(
    val healthService: HealthService
) {

    @GetMapping
    fun healthCheck(): Map<String, Boolean> {
        val isDatabaseHealthy = healthService.isDatabaseHealthy()
        val areServicesHealthy = healthService.areServicesHealthy()
        val isGenAIHealthy = healthService.isGenAiConfigured()


        return mapOf(
            "database" to isDatabaseHealthy,
            "services" to areServicesHealthy,
            "genAI" to isGenAIHealthy
        )
    }
}