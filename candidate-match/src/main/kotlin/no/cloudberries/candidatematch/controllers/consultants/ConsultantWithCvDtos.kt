package no.cloudberries.candidatematch.controllers.consultants

// DTOs used for returning consultants with nested CV structures

data class ConsultantWithCvDto(
    val id: Long?,
    val userId: String,
    val name: String,
    val cvId: String,
    val skills: List<String>,
    val cvs: List<ConsultantCvDto>
)

data class ConsultantCvDto(
    val id: Long?,
    val versionTag: String?,
    val qualityScore: Int?,
    val active: Boolean,
    val keyQualifications: List<KeyQualificationDto>,
    val education: List<EducationDto>,
    val workExperience: List<WorkExperienceDto>,
    val projectExperience: List<ProjectExperienceDto>,
    val certifications: List<CertificationDto>,
    val courses: List<CourseDto>,
    val languages: List<LanguageDto>,
    val skillCategories: List<SkillCategoryDto>,
    val attachments: List<AttachmentDto>
)

data class KeyQualificationDto(
    val label: String?,
    val description: String?
)

data class EducationDto(
    val degree: String?,
    val school: String?,
    val fromYearMonth: String?,
    val toYearMonth: String?
)

data class WorkExperienceDto(
    val employer: String?,
    val fromYearMonth: String?,
    val toYearMonth: String?
)

data class ProjectExperienceDto(
    val customer: String?,
    val description: String?,
    val longDescription: String?,
    val fromYearMonth: String?,
    val toYearMonth: String?,
    val roles: List<ProjectRoleDto>,
    val skills: List<String>
)

data class ProjectRoleDto(
    val name: String?,
    val description: String?
)

data class CertificationDto(
    val name: String?,
    val year: Int?
)

data class CourseDto(
    val name: String?,
    val organizer: String?,
    val year: Int?
)

data class LanguageDto(
    val name: String?,
    val level: String?
)

data class SkillCategoryDto(
    val name: String?,
    val skills: List<SkillInCategoryDto>
)

data class SkillInCategoryDto(
    val name: String?,
    val durationYears: Int?
)

data class AttachmentDto(
    val fileName: String?,
    val url: String?
)
