    package no.cloudberries.barometer.extraction

import no.cloudberries.barometer.domain.CompetencyArea
import no.cloudberries.barometer.domain.CompetencyItem
import no.cloudberries.barometer.domain.DemandPriority
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CompetencyExtractionService {
    private val logger = LoggerFactory.getLogger(CompetencyExtractionService::class.java)
    
    private val competencySystemPrompt = """
        Du er en ekspert på kompetanseanalyse som skal identifisere kompetansekrav fra jobbannonser.
        
        Analyser teksten og identifiser kompetansekrav. Kategoriser som:
        - TECHNICAL: Teknisk arkitektur, API design, ytelsesoptimalisering, testing, sikkerhet
        - DOMAIN: Bransjeerfaring (FinTech, HealthTech), regulatoriske krav, forretningsforståelse  
        - METHODOLOGICAL: Agile, DevOps, Scrum, testing-strategier, continuous integration
        - LEADERSHIP: Teamledelse, mentoring, teknisk kommunikasjon, prosjektledelse

        For hver kompetanse, klassifiser prioritet som:
        - CRITICAL: Absolutt må-krav
        - HIGH: Sterkt ønskelig
        - MEDIUM: Nyttig å kunne  
        - NICE_TO_HAVE: Bonus

        Inkluder erfaring krav der det er spesifisert (f.eks. "3+ års erfaring").

        Returner svar som gyldig JSON med denne strukturen:
        {
            "competencies": [
                {
                    "name": "Microservices architecture",
                    "area": "TECHNICAL",
                    "priority": "HIGH", 
                    "experienceRequired": "3+ years"
                },
                {
                    "name": "Financial domain knowledge",
                    "area": "DOMAIN",
                    "priority": "MEDIUM",
                    "experienceRequired": "Some experience preferred"
                }
            ],
            "context": "Senior technical role requiring architecture and domain expertise",
            "confidence": 0.85
        }
    """.trimIndent()

    fun extractCompetencies(request: CompetencyExtractionRequest): CompetencyExtractionResult {
        logger.info("Mock: Extracting competency requirements from text (length: ${request.text.length})")
        
        // Mock implementation - will be replaced with actual AI integration
        val mockCompetencies = listOf(
            CompetencyItem(
                name = "Microservices architecture",
                area = CompetencyArea.TECHNICAL,
                priority = DemandPriority.HIGH,
                experienceRequired = "3+ years"
            ),
            CompetencyItem(
                name = "Financial domain knowledge",
                area = CompetencyArea.DOMAIN,
                priority = DemandPriority.MEDIUM,
                experienceRequired = "Some experience preferred"
            )
        )
        
        return CompetencyExtractionResult(
            competencies = mockCompetencies,
            context = "Mock competency extraction from ${request.portalSource ?: "unknown"} portal",
            confidence = 0.75
        )
    }
}

class ExtractionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)