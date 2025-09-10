package no.cloudberries.candidatematch.controllers.consultants.dto

// CV and sub-objects

data class YearMonthPeriodDto(
    val fromYear: Int?,
    val fromMonth: Int?,
    val toYear: Int?,
    val toMonth: Int?
)

data class KeyQualificationDto(
    val label: String,
    val description: String
)

data class EducationDto(
    val degree: String,
    val school: String,
    val period: YearMonthPeriodDto
)

data class WorkExperienceDto(
    val employer: String,
    val period: YearMonthPeriodDto
)

data class SkillWithDurationDto(
    val skillName: String,
    val durationInYears: Int?
)

data class ProjectExperienceDto(
    val customer: String,
    val description: String,
    val longDescription: String,
    val period: YearMonthPeriodDto,
    val roles: List<String>,
    val skillsUsed: List<SkillWithDurationDto>
)

data class CertificationDto(
    val name: String,
    val year: Int?
)

data class CourseDto(
    val name: String,
    val organizer: String,
    val year: Int?
)

data class LanguageSkillDto(
    val name: String,
    val level: String
)

data class SkillCategoryDto(
    val name: String,
    val skills: List<SkillWithDurationDto>
)

data class AttachmentDto(
    val fileUrl: String,
    val fileType: String?,
    val fileName: String?,
    val sizeInBytes: Long?
)

data class CvDto(
    val id: Long?,
    val versionTag: String,
    val active: Boolean,
    val qualityScore: Int?,
    val keyQualifications: List<KeyQualificationDto> = emptyList(),
    val educations: List<EducationDto> = emptyList(),
    val workExperiences: List<WorkExperienceDto> = emptyList(),
    val projectExperiences: List<ProjectExperienceDto> = emptyList(),
    val certifications: List<CertificationDto> = emptyList(),
    val courses: List<CourseDto> = emptyList(),
    val languages: List<LanguageSkillDto> = emptyList(),
    val skillCategories: List<SkillCategoryDto> = emptyList(),
    val attachments: List<AttachmentDto> = emptyList()
)

