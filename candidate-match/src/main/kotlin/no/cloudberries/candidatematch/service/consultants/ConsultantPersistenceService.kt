package no.cloudberries.candidatematch.service.consultants

import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.consultant.Consultant
import no.cloudberries.candidatematch.domain.consultant.Cv
import no.cloudberries.candidatematch.infrastructure.adapters.toEntity
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.entities.consultant.*
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.*
import no.cloudberries.candidatematch.infrastructure.repositories.industry.CvProjectExperienceIndustryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConsultantPersistenceService(
    private val consultantRepository: ConsultantRepository,
    private val consultantCvRepository: ConsultantCvRepository,
    private val keyQualificationRepo: CvKeyQualificationRepository,
    private val educationRepo: CvEducationRepository,
    private val workExpRepo: CvWorkExperienceRepository,
    private val projExpRepo: CvProjectExperienceRepository,
    private val projRoleRepo: CvProjectExperienceRoleRepository,
    private val projSkillRepo: CvProjectExperienceSkillRepository,
    private val certRepo: CvCertificationRepository,
    private val courseRepo: CvCourseRepository,
    private val langRepo: CvLanguageRepository,
    private val skillCatRepo: CvSkillCategoryRepository,
    private val skillInCatRepo: CvSkillInCategoryRepository,
    private val attachmentRepo: CvAttachmentRepository,
    private val industryTaggingService: IndustryTaggingService,
    private val cpeiRepository: CvProjectExperienceIndustryRepository,
) {
    
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun persistConsultantWithCv(consultant: Consultant): PersistResult {
        // Persist consultant core
        val savedConsultant = consultantRepository.save(consultant.toEntity())
        // Persist CV header
        val cvRow = consultantCvRepository.save(
            ConsultantCvEntity(
                consultantId = savedConsultant.id!!,
                versionTag = consultant.cv.id,
                qualityScore = consultant.cv.qualityScore,
                active = true,
            )
        )
        // Persist details
        persistCvDetails(cvRow.id!!, consultant.cv)
        return PersistResult(savedConsultant, cvRow)
    }

    @Transactional
    fun upsertConsultantWithCv(consultant: Consultant): UpsertResult {
        val existingConsultant = consultantRepository.findByUserId(consultant.id)
        
        return if (existingConsultant != null) {
            // Update existing consultant
            logger.info { "Updating existing consultant ${consultant.id}" }
            updateExistingConsultant(existingConsultant, consultant)
        } else {
            // Create new consultant
            logger.info { "Creating new consultant ${consultant.id}" }
            createNewConsultant(consultant)
        }
    }
    
    private fun updateExistingConsultant(existingConsultant: ConsultantEntity, consultant: Consultant): UpsertResult {
        // Create updated consultant entity with the existing ID
        val newEntity = consultant.toEntity()
        val updatedEntity = ConsultantEntity(
            id = existingConsultant.id,
            name = newEntity.name,
            userId = newEntity.userId,
            cvId = newEntity.cvId,
            resumeData = newEntity.resumeData,
            createdAt = existingConsultant.createdAt,
            version = existingConsultant.version
        )
        val savedConsultant = consultantRepository.save(updatedEntity)
        
        // Remove all existing CV data for this consultant
        clearExistingCvData(existingConsultant.id!!)
        
        // Create new CV entry
        val cvRow = consultantCvRepository.save(
            ConsultantCvEntity(
                consultantId = savedConsultant.id!!,
                versionTag = consultant.cv.id,
                qualityScore = consultant.cv.qualityScore,
                active = true,
            )
        )
        
        // Add new CV details
        persistCvDetails(cvRow.id!!, consultant.cv)
        
        return UpsertResult(
            consultant = savedConsultant,
            cvHeader = cvRow,
            operation = UpsertOperation.UPDATED
        )
    }
    
    private fun createNewConsultant(consultant: Consultant): UpsertResult {
        val savedConsultant = consultantRepository.save(consultant.toEntity())
        val cvRow = consultantCvRepository.save(
            ConsultantCvEntity(
                consultantId = savedConsultant.id!!,
                versionTag = consultant.cv.id,
                qualityScore = consultant.cv.qualityScore,
                active = true,
            )
        )
        persistCvDetails(cvRow.id!!, consultant.cv)
        
        return UpsertResult(
            consultant = savedConsultant,
            cvHeader = cvRow,
            operation = UpsertOperation.CREATED
        )
    }
    
    private fun clearExistingCvData(consultantId: Long) {
        // Find all CV entries for this consultant
        val existingCvs = consultantCvRepository.findByConsultantId(consultantId)
        val cvIds = existingCvs.mapNotNull { it.id }
        
        if (cvIds.isNotEmpty()) {
            logger.info { "Clearing existing CV data for consultant $consultantId, CV IDs: $cvIds" }
            
            // Clear all CV-related data
            keyQualificationRepo.deleteByCvIdIn(cvIds)
            educationRepo.deleteByCvIdIn(cvIds)
            workExpRepo.deleteByCvIdIn(cvIds)
            
            // Clear project experience data
            val projectExperiences = projExpRepo.findByCvIdIn(cvIds)
            val projectIds = projectExperiences.mapNotNull { it.id }
            if (projectIds.isNotEmpty()) {
                // Clear industry references first to avoid foreign key constraint violations
                cpeiRepository.deleteByProjectExperienceIdIn(projectIds)
                projRoleRepo.deleteByProjectExperienceIdIn(projectIds)
                projSkillRepo.deleteByProjectExperienceIdIn(projectIds)
            }
            projExpRepo.deleteByCvIdIn(cvIds)
            
            certRepo.deleteByCvIdIn(cvIds)
            courseRepo.deleteByCvIdIn(cvIds)
            langRepo.deleteByCvIdIn(cvIds)
            
            // Clear skill categories and skills in category
            val skillCategories = skillCatRepo.findByCvIdIn(cvIds)
            val skillCategoryIds = skillCategories.mapNotNull { it.id }
            if (skillCategoryIds.isNotEmpty()) {
                skillInCatRepo.deleteBySkillCategoryIdIn(skillCategoryIds)
            }
            skillCatRepo.deleteByCvIdIn(cvIds)
            
            attachmentRepo.deleteByCvIdIn(cvIds)
            
            // Finally, delete the CV headers
            consultantCvRepository.deleteAll(existingCvs)
        }
    }

    private fun toYmString(y: java.time.YearMonth?): String? = y?.toString()

    private fun persistCvDetails(cvId: Long, cv: Cv) {
        // Key qualifications
        keyQualificationRepo.saveAll(cv.keyQualifications.map { k ->
            CvKeyQualificationEntity(
                cvId = cvId,
                label = k.label,
                description = k.description,
            )
        })
        // Education
        educationRepo.saveAll(cv.educations.map { e ->
            CvEducationEntity(
                cvId = cvId,
                degree = e.degree,
                school = e.school,
                fromYearMonth = toYmString(e.period.from),
                toYearMonth = toYmString(e.period.to),
            )
        })
        // Work experience
        workExpRepo.saveAll(cv.workExperiences.map { w ->
            CvWorkExperienceEntity(
                cvId = cvId,
                employer = w.employer,
                fromYearMonth = toYmString(w.period.from),
                toYearMonth = toYmString(w.period.to),
            )
        })
        // Project experiences
        val savedProj = projExpRepo.saveAll(cv.projectExperiences.map { p ->
            CvProjectExperienceEntity(
                cvId = cvId,
                customer = p.customer,
                description = p.description,
                longDescription = p.longDescription,
                fromYearMonth = toYmString(p.period.from),
                toYearMonth = toYmString(p.period.to),
            )
        })
        // Link roles and skills to each project experience in order
        cv.projectExperiences.forEachIndexed { idx, p ->
            val pe = savedProj[idx]
            projRoleRepo.saveAll(p.roles.map { r ->
                CvProjectExperienceRoleEntity(
                    projectExperienceId = pe.id!!,
                    name = r.name,
                    description = r.description,
                )
            })
            projSkillRepo.saveAll(p.skillsUsed.map { s ->
                CvProjectExperienceSkillEntity(
                    projectExperienceId = pe.id!!,
                    skill = s.name,
                )
            })
        }
        // Certifications
        certRepo.saveAll(cv.certifications.map { c ->
            CvCertificationEntity(
                cvId = cvId,
                name = c.name,
                year = c.year?.value,
            )
        })
        // Courses
        courseRepo.saveAll(cv.courses.map { c ->
            CvCourseEntity(
                cvId = cvId,
                name = c.name,
                organizer = c.organizer,
                year = c.year?.value,
            )
        })
        // Languages
        langRepo.saveAll(cv.languages.map { l ->
            CvLanguageEntity(
                cvId = cvId,
                name = l.name,
                level = l.level,
            )
        })
        // Skill categories + skills in category
        val savedCats = skillCatRepo.saveAll(cv.skillCategories.map { sc ->
            CvSkillCategoryEntity(
                cvId = cvId,
                name = sc.name,
            )
        })
        cv.skillCategories.forEachIndexed { idx, sc ->
            val cat = savedCats[idx]
            skillInCatRepo.saveAll(sc.skills.map { s ->
                CvSkillInCategoryEntity(
                    skillCategoryId = cat.id!!,
                    name = s.name,
                    durationYears = s.durationInYears,
                )
            })
        }
        // Attachments (none for now)
        
        // After persisting all CV details, tag industries (normalized taxonomy)
        industryTaggingService.tagIndustriesForCv(cvId)
    }

    data class PersistResult(
        val consultant: ConsultantEntity,
        val cvHeader: ConsultantCvEntity,
    )
    
    data class UpsertResult(
        val consultant: ConsultantEntity,
        val cvHeader: ConsultantCvEntity,
        val operation: UpsertOperation
    )
}
