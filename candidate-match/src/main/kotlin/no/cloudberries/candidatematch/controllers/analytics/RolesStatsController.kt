package no.cloudberries.candidatematch.controllers.analytics

import no.cloudberries.candidatematch.service.analytics.RoleStatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/analytics")
class RolesStatsController(
    private val service: RoleStatsService
) {
    @GetMapping("/roles")
    fun stats() = service.getRoleStats()
}