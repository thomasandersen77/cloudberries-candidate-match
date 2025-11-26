package no.cloudberries.candidatematch.matches.domain

import no.cloudberries.candidatematch.domain.ProjectRequest
import java.time.format.DateTimeFormatter

/**
 * Utility object for extracting and formatting requirements from ProjectRequest
 * into a prompt-friendly text format for AI processing.
 * 
 * Follows Single Responsibility Principle by focusing solely on text extraction
 * and formatting for AI consumption.
 */
object RequirementsExtractor {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    
    /**
     * Converts a ProjectRequest domain object into a structured text description
     * suitable for use in AI prompts.
     * 
     * Output format:
     * - Customer name
     * - Project description
     * - Required skills
     * - Project timeline
     * - Response deadline
     * 
     * @param request the project request to extract from
     * @return formatted text description
     */
    fun from(request: ProjectRequest): String {
        val skillsText = if (request.requiredSkills.isNotEmpty()) {
            request.requiredSkills.joinToString(", ") { it.name }
        } else {
            "Ingen spesifikke ferdigheter angitt"
        }
        
        val startDateFormatted = request.startDate.format(dateFormatter)
        val endDateFormatted = request.endDate.format(dateFormatter)
        val deadlineFormatted = request.responseDeadline.format(dateFormatter)
        
        return buildString {
            appendLine("=== PROSJEKTFORESPØRSEL ===")
            appendLine()
            appendLine("KUNDE: ${request.customerName}")
            appendLine()
            appendLine("BESKRIVELSE:")
            appendLine(request.requestDescription)
            appendLine()
            appendLine("PÅKREVDE FERDIGHETER:")
            appendLine(skillsText)
            appendLine()
            appendLine("PROSJEKTPERIODE:")
            appendLine("Fra: $startDateFormatted")
            appendLine("Til: $endDateFormatted")
            appendLine()
            appendLine("SVARFRIST: $deadlineFormatted")
            
            if (request.aISuggestions.isNotEmpty()) {
                appendLine()
                appendLine("TIDLIGERE AI-FORSLAG:")
                request.aISuggestions.take(3).forEachIndexed { index, suggestion ->
                    appendLine("${index + 1}. ${suggestion.consultantName} (${suggestion.matchScore}%): ${suggestion.justification}")
                }
            }
        }.trim()
    }
    
    /**
     * Extracts only the core requirements (description + skills) without metadata.
     * Useful for shorter prompts or when context length is a concern.
     * 
     * @param request the project request to extract from
     * @return compact requirements text
     */
    fun extractCore(request: ProjectRequest): String {
        val skillsText = request.requiredSkills.joinToString(", ") { it.name }
        
        return buildString {
            appendLine(request.requestDescription)
            if (request.requiredSkills.isNotEmpty()) {
                appendLine()
                appendLine("Nøkkelferdigheter: $skillsText")
            }
        }.trim()
    }
    
    /**
     * Extracts requirements in a structured format suitable for must/should/nice-to-have categorization.
     * This would require extending the ProjectRequest model with requirement priorities.
     * 
     * Current implementation uses AI suggestions as a proxy for requirement categorization.
     * 
     * @param request the project request to extract from
     * @return categorized requirements text
     */
    fun extractCategorized(request: ProjectRequest): String {
        // Future enhancement: Add requirement priority field to ProjectRequest
        // For now, we use the main description and skills as "must-have"
        
        val mustHave = buildString {
            appendLine("MÅ-KRAV:")
            request.requiredSkills.forEach { skill ->
                appendLine("  - ${skill.name}")
            }
        }
        
        val description = "KONTEKST: ${request.requestDescription}"
        
        return buildString {
            appendLine(description)
            appendLine()
            appendLine(mustHave)
        }.trim()
    }
}
