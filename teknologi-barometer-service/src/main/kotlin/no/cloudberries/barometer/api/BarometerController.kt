package no.cloudberries.barometer.api

import no.cloudberries.barometer.extraction.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/barometer")
class BarometerController(
    private val technologyExtractionService: TechnologyExtractionService,
    private val profileExtractionService: ProfileExtractionService,
    private val competencyExtractionService: CompetencyExtractionService
) {
    
    @PostMapping("/analyze/technology")
    fun analyzeTechnology(@RequestBody request: TechnologyExtractionRequest): ResponseEntity<TechnologyExtractionResult> {
        val result = technologyExtractionService.extractTechnologies(request)
        return ResponseEntity.ok(result)
    }
    
    @PostMapping("/analyze/profile")
    fun analyzeProfile(@RequestBody request: ProfileExtractionRequest): ResponseEntity<ProfileExtractionResult> {
        val result = profileExtractionService.extractProfile(request)
        return ResponseEntity.ok(result)
    }
    
    @PostMapping("/analyze/competency")
    fun analyzeCompetency(@RequestBody request: CompetencyExtractionRequest): ResponseEntity<CompetencyExtractionResult> {
        val result = competencyExtractionService.extractCompetencies(request)
        return ResponseEntity.ok(result)
    }
    
    @PostMapping("/analyze/all")
    fun analyzeAll(@RequestBody request: AnalyzeAllRequest): ResponseEntity<AnalyzeAllResponse> {
        val technologyResult = technologyExtractionService.extractTechnologies(
            TechnologyExtractionRequest(request.text, request.portalSource)
        )
        
        val profileResult = profileExtractionService.extractProfile(
            ProfileExtractionRequest(request.text, request.portalSource)
        )
        
        val competencyResult = competencyExtractionService.extractCompetencies(
            CompetencyExtractionRequest(request.text, request.portalSource)
        )
        
        return ResponseEntity.ok(
            AnalyzeAllResponse(
                technology = technologyResult,
                profile = profileResult,
                competency = competencyResult
            )
        )
    }
    
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "teknologi-barometer-service",
                "version" to "1.0.0-SNAPSHOT"
            )
        )
    }
}

data class AnalyzeAllRequest(
    val text: String,
    val portalSource: String? = null
)

data class AnalyzeAllResponse(
    val technology: TechnologyExtractionResult,
    val profile: ProfileExtractionResult,
    val competency: CompetencyExtractionResult
)