package no.cloudberries.candidatematch.infrastructure.adapters

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.cloudberries.candidatematch.domain.consultant.*
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.entities.consultant.*
import org.springframework.stereotype.Component

@Component
class ConsultantPersistenceMapper {
    private val mapper = jacksonObjectMapper()

    fun toEntity(domain: Consultant): ConsultantEntity {
        val resumeJson: JsonNode = mapper.readTree(domain.cvAsJson)
        return ConsultantEntity(
            id = null,
            name = domain.personalInfo.name,
            userId = domain.id,
            cvId = domain.cv.id,
            resumeData = resumeJson,
            skills = mutableSetOf()
        )
    }

    fun toCvEntity(consultantEntity: ConsultantEntity, cv: Cv, active: Boolean = true, versionTag: String = "default"): ConsultantCvEntity {
        val cvEntity = ConsultantCvEntity(
            id = null,
            consultant = consultantEntity,
            versionTag = versionTag,
            active = active,
            qualityScore = cv.qualityScore
        )
        // Child collections are added via returned references
        return cvEntity
    }

    fun populateCvChildren(cvEntity: ConsultantCvEntity, cv: Cv) {
        // Key Qualifications
        cv.keyQualifications.forEach {
            cvEntity.keyQualifications.add(
                CvKeyQualificationEntity(
                    id = null,
                    cv = cvEntity,
                    label = it.label,
                    description = it.description
                )
            )
        }
        // Educations
        cv.educations.forEach {
            cvEntity.educations.add(
                CvEducationEntity(
                    id = null,
                    cv = cvEntity,
                    degree = it.degree,
                    school = it.school,
                    period = YearMonthPeriodEmbeddable(
                        fromYear = it.period.from?.year,
                        fromMonth = it.period.from?.monthValue,
                        toYear = it.period.to?.year,
                        toMonth = it.period.to?.monthValue,
                    )
                )
            )
        }
        // Work experiences
        cv.workExperiences.forEach {
            cvEntity.workExperiences.add(
                CvWorkExperienceEntity(
                    id = null,
                    cv = cvEntity,
                    employer = it.employer,
                    period = YearMonthPeriodEmbeddable(
                        fromYear = it.period.from?.year,
                        fromMonth = it.period.from?.monthValue,
                        toYear = it.period.to?.year,
                        toMonth = it.period.to?.monthValue,
                    )
                )
            )
        }
        // Project experiences
        cv.projectExperiences.forEach { pe ->
            val peEntity = CvProjectExperienceEntity(
                id = null,
                cv = cvEntity,
                customer = pe.customer,
                description = pe.description,
                longDescription = pe.longDescription,
                period = YearMonthPeriodEmbeddable(
                    fromYear = pe.period.from?.year,
                    fromMonth = pe.period.from?.monthValue,
                    toYear = pe.period.to?.year,
                    toMonth = pe.period.to?.monthValue,
                )
            )
            peEntity.roles.addAll(pe.roles.map { it.name }.toSet())
            peEntity.skillsUsed.addAll(pe.skillsUsed.map { SkillWithDurationEmbeddable(it.name, it.durationInYears) })
            cvEntity.projectExperiences.add(peEntity)
        }
        // Certifications
        cv.certifications.forEach {
            cvEntity.certifications.add(
                CvCertificationEntity(
                    id = null,
                    cv = cvEntity,
                    name = it.name,
                    year = it.year?.value
                )
            )
        }
        // Courses
        cv.courses.forEach {
            cvEntity.courses.add(
                CvCourseEntity(
                    id = null,
                    cv = cvEntity,
                    name = it.name,
                    organizer = it.organizer,
                    year = it.year?.value
                )
            )
        }
        // Languages
        cv.languages.forEach {
            cvEntity.languages.add(
                CvLanguageSkillEntity(
                    id = null,
                    cv = cvEntity,
                    name = it.name,
                    level = it.level
                )
            )
        }
        // Skill categories
        cv.skillCategories.forEach { sc ->
            val scEntity = CvSkillCategoryEntity(
                id = null,
                cv = cvEntity,
                name = sc.name
            )
            scEntity.skills.addAll(sc.skills.map { SkillWithDurationEmbeddable(it.name, it.durationInYears) })
            cvEntity.skillCategories.add(scEntity)
        }
    }
}

