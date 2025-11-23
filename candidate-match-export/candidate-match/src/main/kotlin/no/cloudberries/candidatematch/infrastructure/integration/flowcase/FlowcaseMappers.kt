package no.cloudberries.candidatematch.infrastructure.integration.flowcase

import no.cloudberries.candidatematch.domain.consultant.*
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeParseException

// A helper function to safely parse year/month strings into a YearMonth object.
private fun toYearMonth(year: String?, month: String?): YearMonth? {
    if (year == null || month == null) return null
    return try {
        // Assumes month is a number e.g., "1" or "01". Adjust format if needed.
        val monthPadded = month.padStart(2, '0')
        YearMonth.parse("$year-$monthPadded")
    } catch (e: DateTimeParseException) {
        // Log the error or handle it as needed, returning null for now.
        null
    }
}

// A helper function to safely parse a year string.
fun Int.toYear(): Year? {
    return try {
        Year.parse(this.toString())
    } catch (_: DateTimeParseException) {
        null
    }
}


fun toYear(year: String?): Year? {
    if (year == null) return null
    return try {
        Year.parse(year)
    } catch (e: DateTimeParseException) {
        null
    }
}


// --- DTO to Domain Mappers ---

fun MultiLangText.toDomain(): String {
    return no ?: int ?: ""
}

fun TechnologySkillDto.toDomain(): Skill {
    return Skill(
        name = this.tags?.toDomain() ?: "",
        durationInYears = this.totalDurationInYears
    )
}

fun TechnologyDto.toDomain(): SkillCategory {
    return SkillCategory(
        name = this.category?.toDomain() ?: "",
        skills = this.technologySkills.filter { !it.disabled }.map { it.toDomain() }
    )
}

fun KeyQualificationDto.toDomain(): KeyQualification {
    return KeyQualification(
        label = this.label?.toDomain() ?: "",
        description = this.longDescription?.toDomain() ?: ""
    )
}

fun WorkExperienceDto.toDomain(): WorkExperience {
    return WorkExperience(
        employer = this.employer?.toDomain() ?: "",
        period = TimePeriod(
            from = toYearMonth(
                this.yearFrom,
                this.monthFrom
            ),
            to = toYearMonth(
                this.yearTo,
                this.monthTo
            )
        )
    )
}

fun RoleDto.toDomain(): Role {
    return Role(
        name = this.name?.toDomain() ?: "",
        description = this.longDescription?.toDomain() ?: ""
    )
}

fun ProjectExperienceDto.toDomain(): ProjectExperience {
    return ProjectExperience(
        customer = this.customer?.toDomain() ?: "",
        description = this.description?.toDomain() ?: "",
        longDescription = this.longDescription?.toDomain() ?: "",
        period = TimePeriod(
            from = toYearMonth(
                this.yearFrom,
                this.monthFrom
            ),
            to = toYearMonth(
                this.yearTo,
                this.monthTo
            )
        ),
        roles = this.roles.map { it.toDomain() },
        skillsUsed = this.projectExperienceSkills.map { it.toDomain() }
    )
}

fun EducationDto.toDomain(): Education {
    return Education(
        degree = this.degree?.toDomain() ?: "",
        school = this.school?.toDomain() ?: "",
        period = TimePeriod(
            from = toYearMonth(
                this.yearFrom,
                "1"
            ), // Assuming start of year if month is absent
            to = toYearMonth(
                this.yearTo,
                "12"
            )      // Assuming end of year if month is absent
        )
    )
}

fun CertificationDto.toDomain(): Certification {
    return Certification(
        name = this.name?.toDomain() ?: "",
        year = toYear(this.year)
    )
}

fun CourseDto.toDomain(): Course {
    return Course(
        name = this.name?.toDomain() ?: "",
        organizer = this.program?.toDomain() ?: "",
        year = toYear(this.year)
    )
}

fun LanguageDto.toDomain(): LanguageSkill {
    return LanguageSkill(
        name = this.name?.toDomain() ?: "",
        level = this.level?.toDomain() ?: ""
    )
}
/*
*//**
 * Maps the summary user information from Flowcase to the Consultant domain object.
 *//*
fun FlowcaseUserDTO.toDomain(): Consultant {
    return Consultant(
        id = this.userId,
        name = this.name,
        email = this.email,
        yearOfBirth = this.bornYear
    )
}

*/
// * Main mapper for the entire CV.

fun FlowcaseCvDto.toDomain(): Cv {
    return Cv(
        id = this.cvId,
        keyQualifications = this.keyQualifications.filter { !it.disabled }.map { it.toDomain() },
        skillCategories = this.technologies.filter { !it.disabled }.map { it.toDomain() },
        workExperiences = this.workExperiences.filter { !it.disabled }.map { it.toDomain() },
        projectExperiences = this.projectExperiences.filter { !it.disabled }.map { it.toDomain() },
        educations = this.educations.filter { !it.disabled }.map { it.toDomain() },
        certifications = this.certifications.filter { !it.disabled }.map { it.toDomain() },
        courses = this.courses.filter { !it.disabled }.map { it.toDomain() },
        languages = this.languages.filter { !it.disabled }.map { it.toDomain() }
    )
}

