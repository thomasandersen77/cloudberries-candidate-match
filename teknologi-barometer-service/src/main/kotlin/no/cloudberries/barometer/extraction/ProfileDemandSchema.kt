package no.cloudberries.barometer.extraction

import no.cloudberries.barometer.domain.LocationType
import no.cloudberries.barometer.domain.RoleType
import no.cloudberries.barometer.domain.SeniorityLevel
import java.util.*

/**
 * Schema for AI extraction of role/profile requirements from job descriptions
 */
data class ProfileExtractionRequest(
    val text: String,
    val portalSource: String? = null
)

data class ProfileExtractionResult(
    val roleName: String,
    val seniorityLevel: SeniorityLevel?,
    val roleType: RoleType,
    val domainArea: String?,
    val locationRequirement: LocationType,
    val teamSize: String?,
    val reportingStructure: String?,
    val languages: List<String>,
    val clearanceLevel: String?,
    val context: String,
    val confidence: Double,
    val extractedAt: Date = Date()
) {
    fun requirementsText(): String {
        val parts = mutableListOf<String>()
        parts.add(roleName)
        seniorityLevel?.let { parts.add(it.value) }
        domainArea?.let { parts.add(it) }
        parts.add(locationRequirement.value)
        parts.addAll(languages)
        clearanceLevel?.let { parts.add(it) }
        return parts.joinToString(", ")
    }
}