package no.cloudberries.barometer.extraction

import no.cloudberries.barometer.domain.CompetencyItem
import java.util.*

/**
 * Schema for AI extraction of competency requirements from job descriptions
 */
data class CompetencyExtractionRequest(
    val text: String,
    val portalSource: String? = null
)

data class CompetencyExtractionResult(
    val competencies: List<CompetencyItem>,
    val context: String,
    val confidence: Double,
    val extractedAt: Date = Date()
) {
    fun requirementsText(): String {
        return competencies.joinToString(", ") { 
            "${it.name} (${it.area.value}, ${it.priority.value})" 
        }
    }
}