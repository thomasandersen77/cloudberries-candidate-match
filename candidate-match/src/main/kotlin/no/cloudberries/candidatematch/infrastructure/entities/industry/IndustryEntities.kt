package no.cloudberries.candidatematch.infrastructure.entities.industry

import jakarta.persistence.*

@Entity
@Table(name = "industry")
data class IndustryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "name", nullable = false, unique = true, length = 150)
    val name: String
)

@Entity
@Table(name = "cv_project_experience_industry")
data class CvProjectExperienceIndustryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "project_experience_id", nullable = false)
    val projectExperienceId: Long,

    @Column(name = "industry_id", nullable = false)
    val industryId: Long,
)
