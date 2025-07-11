package no.cloudberries.candidatematch.integration.flowcase

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowcaseCvDTO(
    @JsonProperty("user_id")
    val userId: String,
    @JsonProperty("default_cv_id")
    val cvId: String,
    @JsonProperty("born_year")
    val bornYear: Int,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("email")
    val email: String,
    // json representasjon av en full cv
    @JsonIgnore
    val flowcaseCvData: String? = null,

) {
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowcaseUserSearchResponse(
    val flowcaseCvDTOList: List<FlowcaseCvDTO>
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowcaseResumeResponse(
    val flowcaseResumeDTOS: List<FlowcaseResumeDTO>
)

