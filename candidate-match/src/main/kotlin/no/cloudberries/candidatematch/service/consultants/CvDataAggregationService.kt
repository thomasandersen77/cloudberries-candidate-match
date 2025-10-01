package no.cloudberries.candidatematch.service.consultants

import no.cloudberries.candidatematch.controllers.consultants.*
import no.cloudberries.candidatematch.domain.candidate.SkillService
import no.cloudberries.candidatematch.infrastructure.entities.consultant.*
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.*
import org.springframework.stereotype.Service

/**
 * Focused service for aggregating CV data.
 * Follows SRP by handling only CV data aggregation concerns.
 */
@Service
class CvDataAggregationService(
    private val consultantCvRepository: ConsultantCvRepository,
    private val cvKeyQualificationRepository: CvKeyQualificationRepository,
    private val cvEducationRepository: CvEducationRepository,
    private val cvWorkExperienceRepository: CvWorkExperienceRepository,
    private val cvProjectExperienceRepository: CvProjectExperienceRepository,
    private val cvProjectExperienceRoleRepository: CvProjectExperienceRoleRepository,
    private val cvProjectExperienceSkillRepository: CvProjectExperienceSkillRepository,
    private val cvCertificationRepository: CvCertificationRepository,
    private val cvCourseRepository: CvCourseRepository,
    private val cvLanguageRepository: CvLanguageRepository,
    private val cvSkillCategoryRepository: CvSkillCategoryRepository,
    private val cvSkillInCategoryRepository: CvSkillInCategoryRepository,
    private val cvAttachmentRepository: CvAttachmentRepository,
    private val industryRepo: no.cloudberries.candidatematch.infrastructure.repositories.industry.IndustryRepository,
    private val cpeiRepo: no.cloudberries.candidatematch.infrastructure.repositories.industry.CvProjectExperienceIndustryRepository,
    private val skillService: SkillService
) {

    /**
     * Aggregates CV data for given consultant IDs
     */
    @no.cloudberries.candidatematch.utils.Timed
    fun aggregateCvData(consultantIds: List<Long>, onlyActiveCv: Boolean): Map<Long, List<ConsultantCvDto>> {
        if (consultantIds.isEmpty()) return emptyMap()

        // Load CVs for all consultants (optionally only active)
        val cvs: List<ConsultantCvEntity> = if (onlyActiveCv) {
            consultantCvRepository.findByConsultantIdInAndActiveTrue(consultantIds)
        } else {
            consultantCvRepository.findByConsultantIdIn(consultantIds)
        }
        if (cvs.isEmpty()) return consultantIds.associateWith { emptyList() }

        val cvIds = cvs.mapNotNull { it.id }
        val cvData = loadCvData(cvIds)

        // Group CVs by consultant and build DTOs
        val cvByConsultant: Map<Long, List<ConsultantCvEntity>> = cvs.groupBy { it.consultantId }
        return consultantIds.associateWith { consultantId ->
            val consultantCvs = cvByConsultant[consultantId] ?: emptyList()
            consultantCvs.map { buildConsultantCvDto(it, cvData) }
        }
    }

    private fun loadCvData(cvIds: List<Long>): CvDataBundle {
        val projectExperiences = cvProjectExperienceRepository.findByCvIdIn(cvIds)
        val projectExperienceIds = projectExperiences.mapNotNull { it.id }

        val projectSkillsByProject: Map<Long?, List<CvProjectExperienceSkillEntity>> =
            if (projectExperienceIds.isEmpty()) emptyMap()
            else cvProjectExperienceSkillRepository.findByProjectExperienceIdIn(projectExperienceIds).groupBy { it.projectExperienceId }

        // Industry mapping: fetch all links then resolve names
        val cpei = if (projectExperienceIds.isEmpty()) emptyList() else cpeiRepo.findByProjectExperienceIdIn(projectExperienceIds)
        val industryIds = cpei.map { it.industryId }.toSet()
        val industriesById = if (industryIds.isEmpty()) emptyMap() else industryRepo.findAllById(industryIds).associateBy { it.id }
        // build industries per cv id
        val industriesByCv: Map<Long?, List<String>> = projectExperiences.groupBy { it.cvId }.mapValues { (_, pes) ->
            pes.flatMap { pe ->
                cpei.filter { it.projectExperienceId == pe.id }.mapNotNull { link -> industriesById[link.industryId]?.name }
            }.distinct()
        }

        return CvDataBundle(
            keyQualificationsByCv = cvKeyQualificationRepository.findByCvIdIn(cvIds).groupBy { it.cvId },
            educationByCv = cvEducationRepository.findByCvIdIn(cvIds).groupBy { it.cvId },
            workExperienceByCv = cvWorkExperienceRepository.findByCvIdIn(cvIds).groupBy { it.cvId },
            projectExperiencesByCv = projectExperiences.groupBy { it.cvId },
            rolesByProject = cvProjectExperienceRoleRepository.findByProjectExperienceIdIn(projectExperienceIds)
                .groupBy { it.projectExperienceId },
            projectSkillsByProject = projectSkillsByProject,
            certificationsByCv = cvCertificationRepository.findByCvIdIn(cvIds).groupBy { it.cvId },
            coursesByCv = cvCourseRepository.findByCvIdIn(cvIds).groupBy { it.cvId },
            languagesByCv = cvLanguageRepository.findByCvIdIn(cvIds).groupBy { it.cvId },
            skillCategoriesByCv = cvSkillCategoryRepository.findByCvIdIn(cvIds).groupBy { it.cvId },
            skillInCategoriesByCategory = loadSkillInCategories(cvIds).mapKeys { it.key as Long? },
            attachmentsByCv = cvAttachmentRepository.findByCvIdIn(cvIds).groupBy { it.cvId },
            industriesByCv = industriesByCv
        )
    }

    private fun loadSkillInCategories(cvIds: List<Long>): Map<Long, List<CvSkillInCategoryEntity>> {
        val skillCategories = cvSkillCategoryRepository.findByCvIdIn(cvIds)
        val skillCategoryIds = skillCategories.mapNotNull { it.id }
        return if (skillCategoryIds.isEmpty()) emptyMap() else
            cvSkillInCategoryRepository.findBySkillCategoryIdIn(skillCategoryIds)
                .groupBy { it.skillCategoryId }
    }

    private fun buildConsultantCvDto(cv: ConsultantCvEntity, cvData: CvDataBundle): ConsultantCvDto {
        val projectExperiences = cvData.projectExperiencesByCv[cv.id] ?: emptyList()

        return ConsultantCvDto(
            id = cv.id,
            versionTag = cv.versionTag,
            qualityScore = cv.qualityScore,
            active = cv.active,
            keyQualifications = cvData.keyQualificationsByCv[cv.id]?.map { it.toDto() } ?: emptyList(),
            education = cvData.educationByCv[cv.id]?.map { it.toDto() } ?: emptyList(),
            workExperience = cvData.workExperienceByCv[cv.id]?.map { it.toDto() } ?: emptyList(),
            projectExperience = projectExperiences.map { buildProjectExperienceDto(it, cvData) },
            certifications = cvData.certificationsByCv[cv.id]?.map { it.toDto() } ?: emptyList(),
            courses = cvData.coursesByCv[cv.id]?.map { it.toDto() } ?: emptyList(),
            languages = cvData.languagesByCv[cv.id]?.map { it.toDto() } ?: emptyList(),
            skillCategories = buildSkillCategoriesDto(cv.id!!, cvData),
            attachments = cvData.attachmentsByCv[cv.id]?.map { it.toDto() } ?: emptyList(),
            industries = cvData.industriesByCv[cv.id] ?: emptyList()
        )
    }

    private fun buildProjectExperienceDto(
        projectExperience: CvProjectExperienceEntity,
        cvData: CvDataBundle
    ): ProjectExperienceDto {
        val projectSkills = cvData.projectSkillsByProject[projectExperience.id]?.mapNotNull { it.skill } ?: emptyList()

        return ProjectExperienceDto(
            customer = projectExperience.customer,
            description = projectExperience.description,
            longDescription = projectExperience.longDescription,
            fromYearMonth = projectExperience.fromYearMonth,
            toYearMonth = projectExperience.toYearMonth,
            roles = cvData.rolesByProject[projectExperience.id]?.map { it.toDto() } ?: emptyList(),
            skills = projectSkills
        )
    }

    private fun buildSkillCategoriesDto(cvId: Long, cvData: CvDataBundle): List<SkillCategoryDto> {
        return cvData.skillCategoriesByCv[cvId]?.map { category ->
            SkillCategoryDto(
                name = category.name,
                skills = cvData.skillInCategoriesByCategory[category.id]?.map { it.toDto() } ?: emptyList()
            )
        } ?: emptyList()
    }
}

/**
 * Data class to bundle all CV-related data for efficient processing
 */
private data class CvDataBundle(
    val keyQualificationsByCv: Map<Long?, List<CvKeyQualificationEntity>>,
    val educationByCv: Map<Long?, List<CvEducationEntity>>,
    val workExperienceByCv: Map<Long?, List<CvWorkExperienceEntity>>,
    val projectExperiencesByCv: Map<Long?, List<CvProjectExperienceEntity>>,
    val rolesByProject: Map<Long?, List<CvProjectExperienceRoleEntity>>,
    val projectSkillsByProject: Map<Long?, List<CvProjectExperienceSkillEntity>>,
    val certificationsByCv: Map<Long?, List<CvCertificationEntity>>,
    val coursesByCv: Map<Long?, List<CvCourseEntity>>,
    val languagesByCv: Map<Long?, List<CvLanguageEntity>>,
    val skillCategoriesByCv: Map<Long?, List<CvSkillCategoryEntity>>,
    val skillInCategoriesByCategory: Map<Long?, List<CvSkillInCategoryEntity>>,
    val attachmentsByCv: Map<Long?, List<CvAttachmentEntity>>,
    val industriesByCv: Map<Long?, List<String>>
)

// Extension functions for DTO conversion
private fun CvKeyQualificationEntity.toDto() = KeyQualificationDto(
    label = this.label,
    description = this.description
)

private fun CvEducationEntity.toDto() = EducationDto(
    degree = this.degree,
    school = this.school,
    fromYearMonth = this.fromYearMonth,
    toYearMonth = this.toYearMonth
)

private fun CvWorkExperienceEntity.toDto() = WorkExperienceDto(
    employer = this.employer,
    fromYearMonth = this.fromYearMonth,
    toYearMonth = this.toYearMonth
)

private fun CvProjectExperienceRoleEntity.toDto() = ProjectRoleDto(
    name = this.name,
    description = this.description
)

private fun CvCertificationEntity.toDto() = CertificationDto(
    name = this.name,
    year = this.year
)

private fun CvCourseEntity.toDto() = CourseDto(
    name = this.name,
    organizer = this.organizer,
    year = this.year
)

private fun CvLanguageEntity.toDto() = LanguageDto(
    name = this.name,
    level = this.level
)

private fun CvSkillInCategoryEntity.toDto() = SkillInCategoryDto(
    name = this.name,
    durationYears = this.durationYears
)

private fun CvAttachmentEntity.toDto() = AttachmentDto(
    fileName = this.fileName,
    url = this.url
)
