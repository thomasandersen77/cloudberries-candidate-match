package no.cloudberries.candidatematch.infrastructure.integration.flowcase

import com.fasterxml.jackson.annotation.JsonIgnore
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowcaseUserDTO(
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
)

data class FlowcaseUserSearchResponse(
    val flowcaseUserDTOs: List<FlowcaseUserDTO>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowcaseCvDto(
    @JsonProperty("_id")
    val cvId: String,
    @JsonProperty("user_id")
    val userId: String,
    val name: String,
    val email: String,
    val telefon: String?,
    @JsonProperty("born_year")
    val bornYear: Int?,
    val title: MultiLangText?,
    val nationality: MultiLangText?,
    @JsonProperty("place_of_residence")
    val placeOfResidence: MultiLangText?,
    val technologies: List<TechnologyDto> = emptyList(),
    @JsonProperty("key_qualifications")
    val keyQualifications: List<KeyQualificationDto> = emptyList(),
    @JsonProperty("work_experiences")
    val workExperiences: List<WorkExperienceDto> = emptyList(),
    @JsonProperty("project_experiences")
    val projectExperiences: List<ProjectExperienceDto> = emptyList(),
    val educations: List<EducationDto> = emptyList(),
    val certifications: List<CertificationDto> = emptyList(),
    val courses: List<CourseDto> = emptyList(),
    val languages: List<LanguageDto> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TechnologyDto(
    val category: MultiLangText?,
    @JsonProperty("technology_skills")
    val technologySkills: List<TechnologySkillDto> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TechnologySkillDto(
    val tags: MultiLangText?,
    @JsonProperty("total_duration_in_years")
    val totalDurationInYears: Int?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KeyQualificationDto(
    val label: MultiLangText?,
    @JsonProperty("long_description")
    val longDescription: MultiLangText?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkExperienceDto(
    val employer: MultiLangText?,
    @JsonProperty("month_from")
    val monthFrom: String?,
    @JsonProperty("month_to")
    val monthTo: String?,
    @JsonProperty("year_from")
    val yearFrom: String?,
    @JsonProperty("year_to")
    val yearTo: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectExperienceDto(
    val customer: MultiLangText?,
    val description: MultiLangText?,
    @JsonProperty("long_description")
    val longDescription: MultiLangText?,
    @JsonProperty("month_from")
    val monthFrom: String?,
    @JsonProperty("month_to")
    val monthTo: String?,
    @JsonProperty("year_from")
    val yearFrom: String?,
    @JsonProperty("year_to")
    val yearTo: String?,
    val roles: List<RoleDto> = emptyList(),
    @JsonProperty("project_experience_skills")
    val projectExperienceSkills: List<TechnologySkillDto> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoleDto(
    val name: MultiLangText?,
    @JsonProperty("long_description")
    val longDescription: MultiLangText?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EducationDto(
    val degree: MultiLangText?,
    val school: MultiLangText?,
    @JsonProperty("year_from")
    val yearFrom: String?,
    @JsonProperty("year_to")
    val yearTo: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CertificationDto(
    val name: MultiLangText?,
    val year: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CourseDto(
    val name: MultiLangText?,
    val program: MultiLangText?,
    val year: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LanguageDto(
    val name: MultiLangText?,
    val level: MultiLangText?
)
