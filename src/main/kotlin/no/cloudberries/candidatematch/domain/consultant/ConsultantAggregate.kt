package no.cloudberries.candidatematch.domain.consultant

import no.cloudberries.candidatematch.domain.Identifiable
import org.springframework.stereotype.Component
import java.time.Year
import java.time.YearMonth

// =================================================================================
// AGGREGATE ROOT
// The main entity and the single entry point for this aggregate.
// All business rules and consistency for the CV are enforced through this class.
// =================================================================================

data class PersonalInfo(
    val name: String,
    val email: String,
    val birthYear: Year?
)

data class Consultant(
    override val id: String,
    val defaultCvId: String,
    val personalInfo: PersonalInfo,
    val cv: Cv,
    val cvAsJson: String, // Useful for persistence without complex mapping
    val skills: List<Skill> = emptyList()
) : Identifiable {

    class Builder(
        private val id: String,
        private val defaultCvId: String
    ) {
        private var personalInfo: PersonalInfo? = null
        private var cv: Cv = Cv(id = defaultCvId) // Default empty CV
        private var cvAsJson: String = ""
        private var skills: List<Skill> = emptyList()

        fun withPersonalInfo(personalInfo: PersonalInfo) = apply {
            this.personalInfo = personalInfo
        }

        fun withCv(cv: Cv) = apply {
            this.cv = cv
        }

        fun withCvAsJson(cvAsJson: String) = apply {
            this.cvAsJson = cvAsJson
        }

        fun withSkills(skills: List<Skill>) = apply {
            this.skills = skills
        }

        fun build(): Consultant {
            requireNotNull(personalInfo) { "Personal info must be set" }
            require(cvAsJson.isNotBlank()) { "CV as JSON must be set" }

            return Consultant(
                id = id,
                defaultCvId = defaultCvId,
                personalInfo = personalInfo!!,
                cv = cv,
                cvAsJson = cvAsJson,
                skills = skills
            )
        }
    }

    companion object {
        fun builder(id: String, defaultCvId: String) = Builder(
            id,
            defaultCvId
        )
    }
}

@Component
class ConsultantFactory {

    fun createConsultant(
        id: String,
        defaultCvId: String,
        name: String,
        email: String,
        birthYear: Year?,
        cv: Cv? = null,
        cvAsJson: String = "",
        skills: List<Skill> = emptyList()
    ): Consultant {
        val personalInfo = PersonalInfo(
            name = name,
            email = email,
            birthYear = birthYear
        )

        return Consultant.builder(
            id,
            defaultCvId
        )
            .withPersonalInfo(personalInfo)
            .withCv(cv ?: Cv(id = defaultCvId))
            .withCvAsJson(cvAsJson)
            .withSkills(skills)
            .build()
    }

    fun updatePersonalInfo(
        consultant: Consultant,
        name: String? = null,
        email: String? = null,
        birthYear: Year? = null
    ): Consultant {
        val updatedPersonalInfo = consultant.personalInfo.copy(
            name = name ?: consultant.personalInfo.name,
            email = email ?: consultant.personalInfo.email,
            birthYear = birthYear ?: consultant.personalInfo.birthYear
        )

        return Consultant.builder(
            consultant.id,
            consultant.defaultCvId
        )
            .withPersonalInfo(updatedPersonalInfo)
            .withCv(consultant.cv)
            .withCvAsJson(consultant.cvAsJson)
            .withSkills(consultant.skills)
            .build()
    }

    fun updateCv(consultant: Consultant, cv: Cv, cvAsJson: String): Consultant {
        return Consultant.builder(
            consultant.id,
            consultant.defaultCvId
        )
            .withPersonalInfo(consultant.personalInfo)
            .withCv(cv)
            .withCvAsJson(cvAsJson)
            .withSkills(consultant.skills)
            .build()
    }

    fun updateSkills(consultant: Consultant, skills: List<Skill>): Consultant {
        return Consultant.builder(
            consultant.id,
            consultant.defaultCvId
        )
            .withPersonalInfo(consultant.personalInfo)
            .withCv(consultant.cv)
            .withCvAsJson(consultant.cvAsJson)
            .withSkills(skills)
            .build()
    }
}


// =================================================================================
// ENTITIES WITHIN THE AGGREGATE
// These objects have their own identity but are managed by the Consultant Aggregate Root.
// Their lifecycle is tied to the Consultant.
// =================================================================================


data class Cv(
    override val id: String,
    val keyQualifications: List<KeyQualification> = emptyList(),
    val workExperiences: List<WorkExperience> = emptyList(),
    val projectExperiences: List<ProjectExperience> = emptyList(),
    val educations: List<Education> = emptyList(),
    val certifications: List<Certification> = emptyList(),
    val courses: List<Course> = emptyList(),
    val languages: List<LanguageSkill> = emptyList(),
    val skillCategories: List<SkillCategory> = emptyList(),
    val qualityScore: Int? = null,
) : Identifiable

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

data class CvUserInfo(
    val userId: String,
    val cvId: String,
    val name: String,
    val bornYear: Int,
    val email: String,
)