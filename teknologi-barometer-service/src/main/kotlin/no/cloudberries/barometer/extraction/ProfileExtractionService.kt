package no.cloudberries.barometer.extraction

import no.cloudberries.barometer.domain.LocationType
import no.cloudberries.barometer.domain.RoleType
import no.cloudberries.barometer.domain.SeniorityLevel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProfileExtractionService {
    private val logger = LoggerFactory.getLogger(ProfileExtractionService::class.java)
    
    private val profileSystemPrompt = """
        Du er en ekspert på rolleanalyse som skal identifisere profil- og rollekrav fra jobbannonser.
        
        Analyser teksten og identifiser:
        - Rolletittel og type (DEVELOPER, ARCHITECT, LEAD, MANAGER, CONSULTANT, SPECIALIST)
        - Senioritetssnivå (JUNIOR, MID, SENIOR, PRINCIPAL, EXPERT)
        - Domeneområde (FinTech, HealthTech, E-commerce, Gaming, etc.)
        - Lokasjonspreferanser (REMOTE, HYBRID, ON_SITE)
        - Språkkrav (Norwegian, English, etc.)
        - Sikkerhetsklarering hvis nevnt
        - Teamstørrelse og rapporteringsstruktur

        Returner svar som gyldig JSON med denne strukturen:
        {
            "roleName": "Senior Backend Developer",
            "seniorityLevel": "SENIOR",
            "roleType": "DEVELOPER", 
            "domainArea": "FinTech",
            "locationRequirement": "HYBRID",
            "teamSize": "5-8 personer",
            "reportingStructure": "Reports to Tech Lead",
            "languages": ["Norwegian", "English"],
            "clearanceLevel": "Hemmelig",
            "context": "Senior development role in financial technology",
            "confidence": 0.90
        }
    """.trimIndent()

    fun extractProfile(request: ProfileExtractionRequest): ProfileExtractionResult {
        logger.info("Mock: Extracting profile requirements from text (length: ${request.text.length})")
        
        // Mock implementation - will be replaced with actual AI integration
        return ProfileExtractionResult(
            roleName = "Senior Backend Developer",
            seniorityLevel = SeniorityLevel.SENIOR,
            roleType = RoleType.DEVELOPER,
            domainArea = "FinTech",
            locationRequirement = LocationType.HYBRID,
            teamSize = "5-8 personer",
            reportingStructure = "Reports to Tech Lead",
            languages = listOf("Norwegian", "English"),
            clearanceLevel = null,
            context = "Mock profile extraction from ${request.portalSource ?: "unknown"} portal",
            confidence = 0.85
        )
    }
}