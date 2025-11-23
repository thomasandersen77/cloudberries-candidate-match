package no.cloudberries.candidatematch.infrastructure.entities

import jakarta.persistence.*

/**
 * Normalized skill entity to avoid duplication of skill names
 */
@Entity
@Table(
    name = "skill",
    indexes = [Index(name = "idx_skill_name", columnList = "name", unique = true)]
)
data class SkillEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "name", nullable = false, unique = true, length = 255)
    val name: String
)

/**
 * Consultant skill association with additional metadata
 */
@Entity
@Table(
    name = "consultant_skill",
    indexes = [
        Index(name = "idx_consultant_skill_consultant", columnList = "consultant_id"),
        Index(name = "idx_consultant_skill_skill", columnList = "skill_id")
    ]
)
data class ConsultantSkillEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "consultant_id", nullable = false)
    val consultantId: Long,
    
    @Column(name = "skill_id", nullable = false)
    val skillId: Long,
    
    @Column(name = "duration_years")
    val durationYears: Int? = null,
)

/**
 * CV project experience skill with reference to normalized skill
 */
@Entity
@Table(
    name = "cv_project_experience_skill_v2",
    indexes = [
        Index(name = "idx_cv_proj_skill_project", columnList = "project_experience_id"),
        Index(name = "idx_cv_proj_skill_skill", columnList = "skill_id")
    ]
)
data class CvProjectSkillEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "project_experience_id", nullable = false)
    val projectExperienceId: Long,
    
    @Column(name = "skill_id", nullable = false)
    val skillId: Long,
    
    @Column(name = "duration_years")
    val durationYears: Int? = null
)