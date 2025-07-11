package no.cloudberries.candidatematch.integration.flowcase

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A private helper class to cleanly deserialize multi-language text fields.
 * It captures both Norwegian ("no") and international/English ("int") values.
 * The public 'text' property implements the fallback logic, preferring Norwegian
 * but using English if the Norwegian value is null. This centralizes the
 * core requirement of the anti-corruption layer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MultiLangText(
    val no: String? = null,
    val int: String? = null
) {
    /**
     * Returns the Norwegian text if available, otherwise falls back to the English text.
     */
    val text: String?
        get() = no ?: int
}

/**
 * Main DTO representing a full, processed CV (Resume) from the Flowcase API.
 *
 * This class acts as the Aggregate Root for the CV data, containing lists of
 * value-object-like DTOs for each section. It is designed with DDD principles
 * to provide a rich, domain-specific model that is clean and easy to use,
 * abstracting away the complexities of the external API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowcaseResumeDTO(
    @JsonProperty("_id")
    val id: String,

    @JsonProperty("user_id")
    val userId: String,

    val name: String,
    val email: String,

    @JsonProperty("born_year")
    val bornYear: Int?,

    // --- CV Sections ---
    @JsonProperty("key_qualifications")
    val keyQualifications: List<KeyQualificationDTO>?,

    val educations: List<EducationDTO>?,

    @JsonProperty("work_experiences")
    val workExperiences: List<WorkExperienceDTO>?,

    @JsonProperty("project_experiences")
    val projectExperiences: List<ProjectExperienceDTO>?,

    val languages: List<LanguageDTO>?,
    val technologies: List<TechnologyCategoryDTO>?,
    val certifications: List<CertificationDTO>?,
    val courses: List<CourseDTO>?,

    @JsonProperty("positions_of_trust")
    val positions: List<PositionDTO>?,

    @JsonProperty("cv_roles")
    val cvRoles: List<CvRoleDTO>?,

    @JsonProperty("highlighted_roles")
    val highlightedRoles: List<HighlightedRoleDTO>?
)

// --- Section-Specific DTOs ---

@JsonIgnoreProperties(ignoreUnknown = true)
data class KeyQualificationDTO(
    @JsonProperty("label") private val labelNode: MultiLangText?,
    @JsonProperty("description") private val descriptionNode: MultiLangText?
) {
    val label: String? get() = labelNode?.text
    val description: String? get() = descriptionNode?.text
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EducationDTO(
    @JsonProperty("degree") private val degreeNode: MultiLangText?,
    @JsonProperty("description") private val descriptionNode: MultiLangText?,
    @JsonProperty("school") private val schoolNode: MultiLangText?,
    @JsonProperty("year_from") val yearFrom: Int?,
    @JsonProperty("year_to") val yearTo: Int?,
    @JsonProperty("month_from") val monthFrom: Int?,
    @JsonProperty("month_to") val monthTo: Int?
) {
    val degree: String? get() = degreeNode?.text
    val description: String? get() = descriptionNode?.text
    val school: String? get() = schoolNode?.text
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkExperienceDTO(
    @JsonProperty("description") private val descriptionNode: MultiLangText?,
    @JsonProperty("employer") private val employerNode: MultiLangText?,
    @JsonProperty("year_from") val yearFrom: Int?,
    @JsonProperty("year_to") val yearTo: Int?,
    @JsonProperty("month_from") val monthFrom: Int?,
    @JsonProperty("month_to") val monthTo: Int?
) {
    val description: String? get() = descriptionNode?.text
    val employer: String? get() = employerNode?.text
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectExperienceDTO(
    @JsonProperty("customer") private val customerNode: MultiLangText?,
    @JsonProperty("customer_description") private val customerDescriptionNode: MultiLangText?,
    @JsonProperty("description") private val descriptionNode: MultiLangText?,
    @JsonProperty("long_description") private val longDescriptionNode: MultiLangText?,
    @JsonProperty("industry") private val industryNode: MultiLangText?,
    @JsonProperty("year_from") val yearFrom: Int?,
    @JsonProperty("year_to") val yearTo: Int?,
    @JsonProperty("month_from") val monthFrom: Int?,
    @JsonProperty("month_to") val monthTo: Int?,
    @JsonProperty("disabled") val isDisabled: Boolean = false
) {
    val customer: String? get() = customerNode?.text
    val customerDescription: String? get() = customerDescriptionNode?.text
    val description: String? get() = descriptionNode?.text
    val longDescription: String? get() = longDescriptionNode?.text
    val industry: String? get() = industryNode?.text
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class LanguageDTO(
    @JsonProperty("language") private val languageNode: MultiLangText?,
    @JsonProperty("level") private val levelNode: MultiLangText?
) {
    val language: String? get() = languageNode?.text
    val level: String? get() = levelNode?.text
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TechnologyCategoryDTO(
    @JsonProperty("category") private val categoryNode: MultiLangText?,
    val skills: List<TechnologySkillDTO>?
) {
    val category: String? get() = categoryNode?.text
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TechnologySkillDTO(
    val label: String?,
    val level: Int?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CertificationDTO(
    @JsonProperty("description") private val descriptionNode: MultiLangText?,
    @JsonProperty("issuer") private val issuerNode: MultiLangText?,
    @JsonProperty("name") private val nameNode: MultiLangText?,
    val year: Int?,
    val month: Int?
) {
    val description: String? get() = descriptionNode?.text
    val issuer: String? get() = issuerNode?.text
    val name: String? get() = nameNode?.text
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CourseDTO(
    @JsonProperty("name") private val nameNode: MultiLangText?,
    @JsonProperty("institution") private val institutionNode: MultiLangText?,
    val year: Int?,
    val month: Int?
) {
    val name: String? get() = nameNode?.text
    val institution: String? get() = institutionNode?.text
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PositionDTO(
    @JsonProperty("title") private val titleNode: MultiLangText?,
    @JsonProperty("year_from") val yearFrom: Int?,
    @JsonProperty("month_from") val monthFrom: Int?
) {
    val title: String? get() = titleNode?.text
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CvRoleDTO(
    @JsonProperty("role") private val roleNode: MultiLangText?,
    @JsonProperty("description") private val descriptionNode: MultiLangText?
) {
    val role: String? get() = roleNode?.text
    val description: String? get() = descriptionNode?.text
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class HighlightedRoleDTO(
    @JsonProperty("no") private val no: String?,
    @JsonProperty("int") private val int: String?
) {
    val name: String? get() = no ?: int
}