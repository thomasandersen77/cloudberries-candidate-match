package no.cloudberries.candidatematch.integration.flowcase

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowcaseCvDTO(
    @JsonProperty("user_id")
    val userId: String,
    @JsonProperty("_id")
    val cvId: String,
    @JsonProperty("born_year")
    val bornYear: Int,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("email")
    val email: String,
) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowcaseUserSearchResponse(
    val flowcaseCvDTOList: List<FlowcaseCvDTO>
)