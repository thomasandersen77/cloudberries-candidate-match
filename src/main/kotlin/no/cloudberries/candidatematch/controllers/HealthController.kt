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
    fun health(): String {
        val isHealthy = healthService.isServiceHealthy() && healthService.isDatabaseHealthy()
        return when (isHealthy) {
            true -> "App is OK"
            false -> "App is NOT OK. Database is not healthy"
        }
    }

}