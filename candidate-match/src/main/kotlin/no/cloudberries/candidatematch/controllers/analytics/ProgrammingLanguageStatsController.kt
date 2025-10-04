package no.cloudberries.candidatematch.controllers.analytics

import no.cloudberries.candidatematch.service.analytics.ProgrammingLanguageStatsService
import no.cloudberries.candidatematch.service.analytics.ProgrammingLanguageStatsService.ProgrammingLanguageStat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/analytics")
class ProgrammingLanguageStatsController(
    private val service: ProgrammingLanguageStatsService
) {
    @GetMapping("/programming-languages")
    fun stats(
        @RequestParam(required = false, name = "languages") languages: List<String>?
    ): List<ProgrammingLanguageStat> = service.getProgrammingLanguageStats(languages)
}