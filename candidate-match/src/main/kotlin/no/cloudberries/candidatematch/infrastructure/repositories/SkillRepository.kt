package no.cloudberries.candidatematch.infrastructure.repositories

import no.cloudberries.candidatematch.infrastructure.entities.SkillEntity
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantSkillEntity
import no.cloudberries.candidatematch.infrastructure.entities.CvProjectSkillEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SkillRepository : JpaRepository<SkillEntity, Long> {
    fun findByName(name: String): SkillEntity?
    
    @Query("SELECT s FROM SkillEntity s WHERE LOWER(s.name) = LOWER(:name)")
    fun findByNameIgnoreCase(@Param("name") name: String): SkillEntity?
    
    fun findByNameIn(names: Collection<String>): List<SkillEntity>
}

@Repository
interface ConsultantSkillRepository : JpaRepository<ConsultantSkillEntity, Long> {
    fun findByConsultantId(consultantId: Long): List<ConsultantSkillEntity>
    fun findByConsultantIdIn(consultantIds: Collection<Long>): List<ConsultantSkillEntity>
    fun deleteByConsultantId(consultantId: Long)
    
    @Query("""
        SELECT cs FROM ConsultantSkillEntity cs 
        JOIN SkillEntity s ON cs.skillId = s.id 
        WHERE cs.consultantId = :consultantId
    """)
    fun findByConsultantIdWithSkill(@Param("consultantId") consultantId: Long): List<ConsultantSkillEntity>
}

@Repository
interface CvProjectSkillRepository : JpaRepository<CvProjectSkillEntity, Long> {
    fun findByProjectExperienceId(projectExperienceId: Long): List<CvProjectSkillEntity>
    fun findByProjectExperienceIdIn(projectExperienceIds: Collection<Long>): List<CvProjectSkillEntity>
    fun deleteByProjectExperienceId(projectExperienceId: Long)
}

/**
 * Projection for consultant skills with skill details
 */
data class ConsultantSkillView(
    val consultantId: Long,
    val skillId: Long,
    val skillName: String,
    val durationYears: Int?,
    val acquiredInProject: String?
)

/**
 * Projection for project skills with skill details
 */
data class ProjectSkillView(
    val projectExperienceId: Long,
    val skillId: Long,
    val skillName: String,
    val durationYears: Int?
)