package no.cloudberries.candidatematch.infrastructure.integration.flowcase

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = MultiLangTextSerializer::class)
data class MultiLangText(
    val no: String? = null,
    val int: String? = null
) {
    val text: String get() = no ?: int ?: ""
}

class MultiLangTextSerializer : JsonSerializer<MultiLangText>() {
    override fun serialize(value: MultiLangText, gen: JsonGenerator, serializers: SerializerProvider) {
        // write empty string if no translation is available
        gen.writeString(value.text)
    }
}

/**
 * Represents the result of fetching a single user from the Flowcase API.
 * It can either be successful (`Found`) or expectedly not found (`NotFound`).
 */
sealed interface FlowcaseUserResponse {
    /**
     * Represents a successful response containing the user's CV data.
     */
    data class Found(val userDTO: FlowcaseUserDTO) : FlowcaseUserResponse

    /**
     * Represents that the user was not found (HTTP 404).
     */
    data object NotFound : FlowcaseUserResponse
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowcaseUserDTO(
    @param:JsonProperty("user_id")
    val userId: String,
    @param:JsonProperty("default_cv_id")
    val cvId: String,
    @param:JsonProperty("born_year")
    val bornYear: Int,
    @param:JsonProperty("name")
    val name: String,
    @param:JsonProperty("email")
    val email: String
)

data class FlowcaseUserSearchResponse(
    val flowcaseUserDTOs: List<FlowcaseUserDTO>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowcaseCvDto(
    @param:JsonProperty("_id")
    val cvId: String,
    @param:JsonProperty("user_id")
    val userId: String,
    val name: String,
    val email: String,
    val telefon: String?,
    @param:JsonProperty("born_year")
    val bornYear: Int?,
    val title: MultiLangText?,
    val nationality: MultiLangText?,
    @param:JsonProperty("place_of_residence")
    val placeOfResidence: MultiLangText?,
    val technologies: List<TechnologyDto> = emptyList(),
    @param:JsonProperty("key_qualifications")
    val keyQualifications: List<KeyQualificationDto> = emptyList(),
    @param:JsonProperty("work_experiences")
    val workExperiences: List<WorkExperienceDto> = emptyList(),
    @param:JsonProperty("project_experiences")
    val projectExperiences: List<ProjectExperienceDto> = emptyList(),
    val educations: List<EducationDto> = emptyList(),
    val certifications: List<CertificationDto> = emptyList(),
    val courses: List<CourseDto> = emptyList(),
    val languages: List<LanguageDto> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TechnologyDto(
    val category: MultiLangText?,
    @param:JsonProperty("technology_skills")
    val technologySkills: List<TechnologySkillDto> = emptyList(),
    val disabled: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TechnologySkillDto(
    val tags: MultiLangText?,
    @param:JsonProperty("total_duration_in_years")
    val totalDurationInYears: Int?,
    val disabled: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KeyQualificationDto(
    val label: MultiLangText?,
    @param:JsonProperty("long_description")
    val longDescription: MultiLangText?,
    val disabled: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkExperienceDto(
    val employer: MultiLangText?,
    @param:JsonProperty("month_from")
    val monthFrom: String?,
    @param:JsonProperty("month_to")
    val monthTo: String?,
    @param:JsonProperty("year_from")
    val yearFrom: String?,
    @param:JsonProperty("year_to")
    val yearTo: String?,
    val disabled: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectExperienceDto(
    val customer: MultiLangText?,
    val description: MultiLangText?,
    @param:JsonProperty("long_description")
    val longDescription: MultiLangText?,
    @param:JsonProperty("month_from")
    val monthFrom: String?,
    @param:JsonProperty("month_to")
    val monthTo: String?,
    @param:JsonProperty("year_from")
    val yearFrom: String?,
    @param:JsonProperty("year_to")
    val yearTo: String?,
    val roles: List<RoleDto> = emptyList(),
    @param:JsonProperty("project_experience_skills")
    val projectExperienceSkills: List<TechnologySkillDto> = emptyList(),
    val disabled: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoleDto(
    val name: MultiLangText?,
    val description: MultiLangText?,
    @param:JsonProperty("long_description")
    val longDescription: MultiLangText?,
    val disabled: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EducationDto(
    val degree: MultiLangText?,
    val school: MultiLangText?,
    @param:JsonProperty("year_from")
    val yearFrom: String?,
    @param:JsonProperty("year_to")
    val yearTo: String?,
    val disabled: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CertificationDto(
    val name: MultiLangText?,
    val year: String?,
    val disabled: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CourseDto(
    val name: MultiLangText?,
    val program: MultiLangText?,
    val year: String?,
    val disabled: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LanguageDto(
    val name: MultiLangText?,
    val level: MultiLangText?,
    val disabled: Boolean = false,
)
