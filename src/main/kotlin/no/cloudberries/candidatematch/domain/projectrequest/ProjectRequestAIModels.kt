package no.cloudberries.candidatematch.domain.projectrequest

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectRequestAIResponse(
    @JsonProperty("project_request")
    val projectRequest: ProjectRequestPayload
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectRequestPayload(
    @JsonProperty("customer_name")
    val customerName: String? = null,
    val title: String? = null,
    val summary: String? = null,
    @JsonProperty("must_requirements")
    val mustRequirements: List<ProjectRequirementPayload> = emptyList(),
    @JsonProperty("should_requirements")
    val shouldRequirements: List<ProjectRequirementPayload> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectRequirementPayload(
    val name: String,
    val details: String? = null,
)