package no.cloudberries.barometer.extraction

import no.cloudberries.barometer.domain.TechItem
import java.util.*

/**
 * Schema for AI extraction of technology requirements from job descriptions
 */
data class TechnologyExtractionRequest(
    val text: String,
    val portalSource: String? = null
)

data class TechnologyExtractionResult(
    val technologies: List<TechItem>,
    val context: String,
    val confidence: Double,
    val extractedAt: Date = Date()
) {
    fun requirementsText(): String {
        return technologies.joinToString(", ") { "${it.name} (${it.priority.value})" }
    }
}