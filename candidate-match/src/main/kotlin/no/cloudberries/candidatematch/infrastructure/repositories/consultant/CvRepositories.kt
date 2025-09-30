package no.cloudberries.candidatematch.infrastructure.repositories.consultant

import no.cloudberries.candidatematch.infrastructure.entities.consultant.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository interface CvKeyQualificationRepository : JpaRepository<CvKeyQualificationEntity, Long> {
    fun findByCvIdIn(cvIds: Collection<Long>): List<CvKeyQualificationEntity>
    fun deleteByCvIdIn(cvIds: Collection<Long>): Int
}
@Repository interface CvEducationRepository : JpaRepository<CvEducationEntity, Long> {
    fun findByCvIdIn(cvIds: Collection<Long>): List<CvEducationEntity>
    fun deleteByCvIdIn(cvIds: Collection<Long>): Int
}
@Repository interface CvWorkExperienceRepository : JpaRepository<CvWorkExperienceEntity, Long> {
    fun findByCvIdIn(cvIds: Collection<Long>): List<CvWorkExperienceEntity>
    fun deleteByCvIdIn(cvIds: Collection<Long>): Int
}
@Repository interface CvProjectExperienceRepository : JpaRepository<CvProjectExperienceEntity, Long> {
    fun findByCvIdIn(cvIds: Collection<Long>): List<CvProjectExperienceEntity>
    fun deleteByCvIdIn(cvIds: Collection<Long>): Int
}
@Repository interface CvProjectExperienceRoleRepository : JpaRepository<CvProjectExperienceRoleEntity, Long> {
    fun findByProjectExperienceIdIn(projectExperienceIds: Collection<Long>): List<CvProjectExperienceRoleEntity>
    fun deleteByProjectExperienceIdIn(projectExperienceIds: Collection<Long>): Int
}
@Repository interface CvProjectExperienceSkillRepository : JpaRepository<CvProjectExperienceSkillEntity, Long> {
    fun findByProjectExperienceIdIn(projectExperienceIds: Collection<Long>): List<CvProjectExperienceSkillEntity>
    fun deleteByProjectExperienceIdIn(projectExperienceIds: Collection<Long>): Int
}
@Repository interface CvCertificationRepository : JpaRepository<CvCertificationEntity, Long> {
    fun findByCvIdIn(cvIds: Collection<Long>): List<CvCertificationEntity>
    fun deleteByCvIdIn(cvIds: Collection<Long>): Int
}
@Repository interface CvCourseRepository : JpaRepository<CvCourseEntity, Long> {
    fun findByCvIdIn(cvIds: Collection<Long>): List<CvCourseEntity>
    fun deleteByCvIdIn(cvIds: Collection<Long>): Int
}
@Repository interface CvLanguageRepository : JpaRepository<CvLanguageEntity, Long> {
    fun findByCvIdIn(cvIds: Collection<Long>): List<CvLanguageEntity>
    fun deleteByCvIdIn(cvIds: Collection<Long>): Int
}
@Repository interface CvSkillCategoryRepository : JpaRepository<CvSkillCategoryEntity, Long> {
    fun findByCvIdIn(cvIds: Collection<Long>): List<CvSkillCategoryEntity>
    fun deleteByCvIdIn(cvIds: Collection<Long>): Int
}
@Repository interface CvSkillInCategoryRepository : JpaRepository<CvSkillInCategoryEntity, Long> {
    fun findBySkillCategoryIdIn(categoryIds: Collection<Long>): List<CvSkillInCategoryEntity>
    fun deleteBySkillCategoryIdIn(categoryIds: Collection<Long>): Int
}
@Repository interface CvAttachmentRepository : JpaRepository<CvAttachmentEntity, Long> {
    fun findByCvIdIn(cvIds: Collection<Long>): List<CvAttachmentEntity>
    fun deleteByCvIdIn(cvIds: Collection<Long>): Int
}
