package no.cloudberries.candidatematch.controllers.consultants

import no.cloudberries.candidatematch.controllers.consultants.dto.*
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.entities.consultant.*

object ConsultantDtoMapper {
    fun toSummaryDto(entity: ConsultantEntity, activeCv: ConsultantCvEntity?): ConsultantSummaryDto =
        ConsultantSummaryDto(
            id = entity.id!!,
            name = entity.name,
            userId = entity.userId,
            defaultCvId = entity.cvId,
            activeCvEntityId = activeCv?.id
        )

    fun toCvDto(cv: ConsultantCvEntity): CvDto = CvDto(
        id = cv.id,
        versionTag = cv.versionTag,
        active = cv.active,
        qualityScore = cv.qualityScore,
        keyQualifications = cv.keyQualifications.map { KeyQualificationDto(it.label, it.description) },
        educations = cv.educations.map {
            EducationDto(
                degree = it.degree,
                school = it.school,
                period = YearMonthPeriodDto(
                    fromYear = it.period.fromYear,
                    fromMonth = it.period.fromMonth,
                    toYear = it.period.toYear,
                    toMonth = it.period.toMonth
                )
            )
        },
        workExperiences = cv.workExperiences.map {
            WorkExperienceDto(
                employer = it.employer,
                period = YearMonthPeriodDto(
                    fromYear = it.period.fromYear,
                    fromMonth = it.period.fromMonth,
                    toYear = it.period.toYear,
                    toMonth = it.period.toMonth
                )
            )
        },
        projectExperiences = cv.projectExperiences.map { pe ->
            ProjectExperienceDto(
                customer = pe.customer,
                description = pe.description,
                longDescription = pe.longDescription,
                period = YearMonthPeriodDto(
                    fromYear = pe.period.fromYear,
                    fromMonth = pe.period.fromMonth,
                    toYear = pe.period.toYear,
                    toMonth = pe.period.toMonth
                ),
                roles = pe.roles.toList(),
                skillsUsed = pe.skillsUsed.map { SkillWithDurationDto(it.skillName, it.durationInYears) }
            )
        },
        certifications = cv.certifications.map { CertificationDto(it.name, it.year) },
        courses = cv.courses.map { CourseDto(it.name, it.organizer, it.year) },
        languages = cv.languages.map { LanguageSkillDto(it.name, it.level) },
        skillCategories = cv.skillCategories.map { sc ->
            SkillCategoryDto(sc.name, sc.skills.map { SkillWithDurationDto(it.skillName, it.durationInYears) })
        },
        attachments = cv.attachments.map { AttachmentDto(it.fileUrl, it.fileType, it.fileName, it.sizeInBytes) }
    )

    fun fromDto(consultant: ConsultantEntity, dto: CvDto): ConsultantCvEntity {
        val cv = ConsultantCvEntity(
            id = dto.id,
            consultant = consultant,
            versionTag = dto.versionTag,
            active = dto.active,
            qualityScore = dto.qualityScore
        )
        // children
        dto.keyQualifications.forEach { k ->
            cv.keyQualifications.add(CvKeyQualificationEntity(id = null, cv = cv, label = k.label, description = k.description))
        }
        dto.educations.forEach { e ->
            cv.educations.add(
                CvEducationEntity(
                    id = null,
                    cv = cv,
                    degree = e.degree,
                    school = e.school,
                    period = YearMonthPeriodEmbeddable(e.period.fromYear, e.period.fromMonth, e.period.toYear, e.period.toMonth)
                )
            )
        }
        dto.workExperiences.forEach { w ->
            cv.workExperiences.add(
                CvWorkExperienceEntity(
                    id = null,
                    cv = cv,
                    employer = w.employer,
                    period = YearMonthPeriodEmbeddable(w.period.fromYear, w.period.fromMonth, w.period.toYear, w.period.toMonth)
                )
            )
        }
        dto.projectExperiences.forEach { p ->
            val pe = CvProjectExperienceEntity(
                id = null,
                cv = cv,
                customer = p.customer,
                description = p.description,
                longDescription = p.longDescription,
                period = YearMonthPeriodEmbeddable(p.period.fromYear, p.period.fromMonth, p.period.toYear, p.period.toMonth)
            )
            pe.roles.addAll(p.roles)
            pe.skillsUsed.addAll(p.skillsUsed.map { SkillWithDurationEmbeddable(it.skillName, it.durationInYears) })
            cv.projectExperiences.add(pe)
        }
        dto.certifications.forEach { c ->
            cv.certifications.add(CvCertificationEntity(id = null, cv = cv, name = c.name, year = c.year))
        }
        dto.courses.forEach { c ->
            cv.courses.add(CvCourseEntity(id = null, cv = cv, name = c.name, organizer = c.organizer, year = c.year))
        }
        dto.languages.forEach { l ->
            cv.languages.add(CvLanguageSkillEntity(id = null, cv = cv, name = l.name, level = l.level))
        }
        dto.skillCategories.forEach { sc ->
            val sce = CvSkillCategoryEntity(id = null, cv = cv, name = sc.name)
            sce.skills.addAll(sc.skills.map { SkillWithDurationEmbeddable(it.skillName, it.durationInYears) })
            cv.skillCategories.add(sce)
        }
        dto.attachments.forEach { a ->
            cv.attachments.add(CvAttachmentEntity(id = null, cv = cv, fileUrl = a.fileUrl, fileType = a.fileType, fileName = a.fileName, sizeInBytes = a.sizeInBytes))
        }
        return cv
    }
}

