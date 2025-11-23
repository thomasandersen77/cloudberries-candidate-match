package no.cloudberries.candidatematch.service.projectrequest

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Data classes for parsing AI JSON response from AnalyzeCustomerRequestPromptTemplate
 */
data class ProjectRequestAIResponse(
    val id: Long? = null,
    val filename: String? = null,
    @JsonProperty("Upload date")
    val uploadDate: String? = null,
    @JsonProperty("Deadline Date") 
    val deadlineDate: String? = null,
    @JsonProperty("Summary")
    val summary: String? = null,
    @JsonProperty("Requirements")
    val requirements: RequirementsSection? = null,
    // Alternative field names in case AI uses different casing
    @JsonProperty("summary")
    val summaryAlt: String? = null,
    @JsonProperty("requirements") 
    val requirementsAlt: RequirementsSection? = null,
    // Customer name extraction (not in original template but needed)
    @JsonProperty("customer_name")
    val customerName: String? = null,
    @JsonProperty("Customer Name")
    val customerNameAlt: String? = null
)

data class RequirementsSection(
    @JsonProperty("MUST")
    val must: List<String>? = null,
    @JsonProperty("SHOULD") 
    val should: List<String>? = null,
    // Alternative field names
    @JsonProperty("must")
    val mustAlt: List<String>? = null,
    @JsonProperty("should")
    val shouldAlt: List<String>? = null
)

/**
 * Processed AI response with normalized data
 */
data class ProcessedAIResponse(
    val customerName: String?,
    val summary: String?,
    val mustRequirements: List<String>,
    val shouldRequirements: List<String>,
    val uploadedAt: LocalDateTime,
    val deadlineDate: LocalDateTime?
)