package no.cloudberries.candidatematch.domain.consultant

import java.time.Year
import java.time.YearMonth

// =================================================================================
// AGGREGATE ROOT
// The main entity and the single entry point for this aggregate.
// All business rules and consistency for the CV are enforced through this class.
// =================================================================================

data class Consultant(
    val id: String, // e.g., user_id from Flowcase
    val defaultCvId: String,
    val name: String,
    val email: String,
    val birthYear: Year,
    val cv: Cv,
    val cvAsJson: String, // Useful for persistence without complex mapping
    val skills: List<Skill> = mutableListOf()
)

// =================================================================================
// ENTITIES WITHIN THE AGGREGATE
// These objects have their own identity but are managed by the Consultant Aggregate Root.
// Their lifecycle is tied to the Consultant.
// =================================================================================

data class Cv(
    val id: String,
    val consultantId: String,
    val title: String,
    val nationality: String,
    val residence: String,
    val keyQualifications: List<KeyQualification>,
    val skillCategories: List<SkillCategory>,
    val workExperiences: List<WorkExperience>,
    val projectExperiences: List<ProjectExperience>,
    val educations: List<Education>,
    val certifications: List<Certification>,
    val courses: List<Course>,
    val languages: List<LanguageSkill>
)

data class ProjectExperience(
    val customer: String,
    val description: String,
    val longDescription: String,
    val period: TimePeriod,
    val roles: List<Role>,
    val skillsUsed: List<Skill>
)

data class Education(
    val degree: String,
    val school: String,
    val period: TimePeriod
)


// =================================================================================
// VALUE OBJECTS
// These objects are defined by their attributes, not an ID. They are immutable.
// They describe characteristics of the entities.
// =================================================================================

data class TimePeriod(
    val from: YearMonth?,
    val to: YearMonth?
)

data class Skill(
    val name: String,
    val durationInYears: Int?
)

data class KeyQualification(
    val label: String,
    val description: String
)

data class SkillCategory(
    val name: String,
    val skills: List<Skill>
)

data class WorkExperience(
    val employer: String,
    val period: TimePeriod
)

data class Role(
    val name: String,
    val description: String
)

data class Certification(
    val name: String,
    val year: Year?
)

data class Course(
    val name: String,
    val organizer: String,
    val year: Year?
)

data class LanguageSkill(
    val name: String,
    val level: String
)