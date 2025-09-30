package no.cloudberries.candidatematch.domain

import java.time.LocalDateTime

data class AISuggestion(
    val id: Long? = null,
    val consultantName: String,
    val userId: String,
    val cvId: String,
    val matchScore: Double,
    val justification: String,
    val projectRequest: ProjectRequest? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val skills: List<String> = emptyList()
) {
    fun scoreIsValid(): Boolean = matchScore in 0.0..100.0
    
    fun hasAcceptableScore(minimumScore: Double = 70.0): Boolean = matchScore >= minimumScore
    
    fun summarizeMatch(): String {
        val scoreDescription = when {  
            matchScore >= 90.0 -> "excellent"
            matchScore >= 80.0 -> "very good"
            matchScore >= 70.0 -> "good"
            matchScore >= 60.0 -> "acceptable"
            else -> "poor"
        }
        
        return "$consultantName has a $scoreDescription match (${matchScore.toInt()}%) with the project."
    }
}
