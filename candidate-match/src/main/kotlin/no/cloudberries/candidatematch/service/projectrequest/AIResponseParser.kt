package no.cloudberries.candidatematch.service.projectrequest

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
class AIResponseParser(
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}
    
    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    )

    /**
     * Attempts to parse AI response as JSON and extract structured data
     * Falls back gracefully if parsing fails
     */
    fun parseAIResponse(
        aiResponse: String,
        originalText: String,
        originalFilename: String?
    ): ProcessedAIResponse {
        val fallback = createFallbackResponse(originalText, originalFilename)
        
        if (aiResponse.isBlank()) {
            logger.warn { "AI response is blank, using fallback" }
            return fallback
        }

        return try {
            // Try to extract JSON from AI response (it might have extra text)
            val jsonContent = extractJsonFromResponse(aiResponse)
            if (jsonContent.isBlank()) {
                logger.warn { "No JSON content found in AI response, using fallback" }
                return fallback
            }
            
            val aiResponseData = objectMapper.readValue(jsonContent, ProjectRequestAIResponse::class.java)
            logger.info { "Successfully parsed AI JSON response" }
            
            ProcessedAIResponse(
                customerName = extractCustomerName(aiResponseData, originalText),
                summary = extractSummary(aiResponseData) ?: fallback.summary,
                mustRequirements = extractMustRequirements(aiResponseData),
                shouldRequirements = extractShouldRequirements(aiResponseData),
                uploadedAt = LocalDateTime.now(),
                deadlineDate = parseDeadlineDate(aiResponseData)
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse AI response as JSON, using fallback: ${e.message}" }
            fallback
        }
    }

    private fun extractJsonFromResponse(response: String): String {
        // Try to find JSON content within the response
        val jsonStart = response.indexOf('{')
        val jsonEnd = response.lastIndexOf('}')
        
        if (jsonStart == -1 || jsonEnd == -1 || jsonStart >= jsonEnd) {
            return ""
        }
        
        return response.substring(jsonStart, jsonEnd + 1)
    }

    private fun extractCustomerName(aiResponse: ProjectRequestAIResponse, originalText: String): String? {
        // Try to get customer name from AI response
        val aiCustomerName = aiResponse.customerName ?: aiResponse.customerNameAlt
        if (!aiCustomerName.isNullOrBlank()) {
            return aiCustomerName.trim()
        }
        
        // Try to extract customer name from original text using simple patterns
        val lines = originalText.lines().map { it.trim() }
        
        // Look for common patterns like "Kunde:", "Customer:", "For:", etc.
        val customerPatterns = listOf(
            Regex("(?i)kunde[:\\s]+([^\\n\\r]+)", RegexOption.IGNORE_CASE),
            Regex("(?i)customer[:\\s]+([^\\n\\r]+)", RegexOption.IGNORE_CASE),
            Regex("(?i)for[:\\s]+([^\\n\\r]+)", RegexOption.IGNORE_CASE),
            Regex("(?i)klient[:\\s]+([^\\n\\r]+)", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in customerPatterns) {
            val match = pattern.find(originalText)
            if (match != null) {
                val extracted = match.groupValues[1].trim()
                if (extracted.isNotBlank() && extracted.length < 100) {
                    return extracted
                }
            }
        }
        
        // Fallback: if we can't extract customer name, use "Unknown Customer"
        return "Unknown Customer"
    }

    private fun extractSummary(aiResponse: ProjectRequestAIResponse): String? {
        return (aiResponse.summary ?: aiResponse.summaryAlt)?.trim()
    }

    private fun extractMustRequirements(aiResponse: ProjectRequestAIResponse): List<String> {
        val requirements = aiResponse.requirements ?: aiResponse.requirementsAlt
        val must = requirements?.must ?: requirements?.mustAlt
        return must?.filter { it.isNotBlank() }?.map { it.trim() } ?: emptyList()
    }

    private fun extractShouldRequirements(aiResponse: ProjectRequestAIResponse): List<String> {
        val requirements = aiResponse.requirements ?: aiResponse.requirementsAlt
        val should = requirements?.should ?: requirements?.shouldAlt
        return should?.filter { it.isNotBlank() }?.map { it.trim() } ?: emptyList()
    }

    private fun parseDeadlineDate(aiResponse: ProjectRequestAIResponse): LocalDateTime? {
        val deadlineDateStr = aiResponse.deadlineDate
        if (deadlineDateStr.isNullOrBlank()) return null
        
        for (formatter in dateFormats) {
            try {
                return LocalDateTime.parse(deadlineDateStr, formatter)
            } catch (e: DateTimeParseException) {
                // Try next format
            }
        }
        
        logger.warn { "Could not parse deadline date: $deadlineDateStr" }
        return null
    }

    private fun createFallbackResponse(originalText: String, originalFilename: String?): ProcessedAIResponse {
        val lines = originalText.lines().map { it.trim() }.filter { it.isNotBlank() }
        
        return ProcessedAIResponse(
            customerName = "Unknown Customer",
            summary = originalText.take(1000),
            mustRequirements = emptyList(),
            shouldRequirements = emptyList(),
            uploadedAt = LocalDateTime.now(),
            deadlineDate = null
        )
    }
}