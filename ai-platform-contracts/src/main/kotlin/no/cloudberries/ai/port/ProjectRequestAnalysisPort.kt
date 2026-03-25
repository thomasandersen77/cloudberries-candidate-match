package no.cloudberries.ai.port

import java.time.LocalDateTime

interface ProjectRequestAnalysisPort {
    fun analyzeProjectRequest(text: String, originalFilename: String? = null): AnalyzedProjectRequest
}

data class AnalyzedProjectRequest(
    val customerName: String?,
    val summary: String?,
    val mustRequirements: List<String>,
    val shouldRequirements: List<String>,
    val uploadedAt: LocalDateTime,
    val deadlineDate: LocalDateTime?
)
