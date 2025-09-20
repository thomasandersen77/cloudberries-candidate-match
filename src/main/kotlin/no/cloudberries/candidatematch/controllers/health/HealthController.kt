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

    data class HealthResponse(val status: String, val details: Map<String, Any>)

    @GetMapping
    fun healthCheck(): HealthResponse {
        val up = healthService.checkOverallHealth()
        val rawDetails = healthService.getHealthDetails()
        val normalized = rawDetails.mapValues { (_, v) ->
            when (v) {
                is Boolean -> if (v) "UP" else "DOWN"
                is String -> v
                else -> v.toString()
            }
        }
        return HealthResponse(
            status = if (up) "UP" else "DOWN",
            details = normalized
        )
    }
}
