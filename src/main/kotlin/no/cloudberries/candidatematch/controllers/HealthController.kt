package no.cloudberries.candidatematch.controllers

import no.cloudberries.candidatematch.service.HealthService
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
        val databaseHealthy = healthService.isDatabaseHealthy()
        val servicesHealthy = healthService.isServiceHealthy()
        val isGenAIHealthy = healthService.checkGenAiHealth()


        return mapOf(
            "database" to databaseHealthy,
            "services" to servicesHealthy,
            "genAI" to isGenAIHealthy
        )
    }
}