package no.cloudberries.barometer.extraction

import no.cloudberries.barometer.domain.DemandPriority
import no.cloudberries.barometer.domain.TechItem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TechnologyExtractionService {
    private val logger = LoggerFactory.getLogger(TechnologyExtractionService::class.java)
    
    private val technologySystemPrompt = """
        Du er en ekspert på teknologianalyse som skal identifisere teknologikrav fra jobbannonser.
        
        Analyser teksten og identifiser teknologikrav. Fokuser på:
        - Programmeringsspråk (Java, Kotlin, Python, TypeScript, etc.)
        - Rammeverk og biblioteker (Spring Boot, React, Angular, Vue, etc.)
        - Databaser og middleware (PostgreSQL, MongoDB, Redis, Kafka, etc.)
        - Cloud og infrastruktur (AWS, Azure, GCP, Kubernetes, Docker, etc.)
        - Utviklingsverktøy (Git, Jenkins, Maven, npm, etc.)

        For hver teknologi, klassifiser prioritet som:
        - CRITICAL: Absolutt må-krav, ikke forhandlingsbart
        - HIGH: Sterkt ønskelig, viktig for rollen
        - MEDIUM: Nyttig å kunne, fordel
        - NICE_TO_HAVE: Bonus, ikke nødvendig

        Kategoriser teknologier som:
        - programming_language, framework, database, cloud_platform, tool, methodology

        Returner svar som gyldig JSON med denne strukturen:
        {
            "technologies": [
                {
                    "name": "Kotlin",
                    "category": "programming_language", 
                    "priority": "CRITICAL",
                    "context": "Backend services development"
                }
            ],
            "context": "Backend development role requiring modern JVM technologies",
            "confidence": 0.85
        }
    """.trimIndent()

    fun extractTechnologies(request: TechnologyExtractionRequest): TechnologyExtractionResult {
        logger.info("Mock: Extracting technology requirements from text (length: ${request.text.length})")
        
        // Mock implementation - will be replaced with actual AI integration
        val mockTechnologies = listOf(
            TechItem(
                name = "Kotlin",
                category = "programming_language",
                priority = DemandPriority.CRITICAL,
                context = "Backend development"
            ),
            TechItem(
                name = "Spring Boot",
                category = "framework",
                priority = DemandPriority.HIGH,
                context = "Java backend framework"
            )
        )
        
        return TechnologyExtractionResult(
            technologies = mockTechnologies,
            context = "Mock extraction from ${request.portalSource ?: "unknown"} portal",
            confidence = 0.80
        )
    }
}