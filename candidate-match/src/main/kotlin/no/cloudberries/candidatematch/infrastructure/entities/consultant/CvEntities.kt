package no.cloudberries.candidatematch.infrastructure.entities.consultant

import jakarta.persistence.*

@Entity
@Table(name = "consultant_cv")
data class ConsultantCvEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "consultant_id",
        nullable = false
    )
    val consultantId: Long,

    @Column(name = "version_tag")
    val versionTag: String? = null,

    @Column(name = "quality_score")
    val qualityScore: Int? = null,

    @Column(name = "active")
    val active: Boolean = false,

    @Column(name = "gemini_file_uri", length = 512)
    var geminiFileUri: String? = null,
)

@Entity
@Table(name = "cv_key_qualification")
data class CvKeyQualificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "cv_id",
        nullable = false
    )
    val cvId: Long,

    val label: String? = null,
    @Column(columnDefinition = "text")
    val description: String? = null,
)

@Entity
@Table(name = "cv_education")
data class CvEducationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "cv_id",
        nullable = false
    )
    val cvId: Long,

    val degree: String? = null,
    val school: String? = null,
    @Column(name = "from_year_month")
    val fromYearMonth: String? = null,
    @Column(name = "to_year_month")
    val toYearMonth: String? = null,
)

@Entity
@Table(name = "cv_work_experience")
data class CvWorkExperienceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "cv_id",
        nullable = false
    )
    val cvId: Long,

    val employer: String? = null,
    @Column(name = "from_year_month")
    val fromYearMonth: String? = null,
    @Column(name = "to_year_month")
    val toYearMonth: String? = null,
)

@Entity
@Table(name = "cv_project_experience")
data class CvProjectExperienceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "cv_id",
        nullable = false
    )
    val cvId: Long,

    val customer: String? = null,
    @Column(columnDefinition = "text")
    val description: String? = null,
    @Column(name = "long_description", columnDefinition = "text")
    val longDescription: String? = null,
    @Column(name = "from_year_month")
    val fromYearMonth: String? = null,
    @Column(name = "to_year_month")
    val toYearMonth: String? = null,
)

@Entity
@Table(name = "cv_project_experience_role")
data class CvProjectExperienceRoleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "project_experience_id",
        nullable = false
    )
    val projectExperienceId: Long,

    val name: String? = null,
    @Column(columnDefinition = "text")
    val description: String? = null,
)

@Entity
@Table(name = "cv_project_experience_skill")
data class CvProjectExperienceSkillEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "project_experience_id",
        nullable = false
    )
    val projectExperienceId: Long,

    @Column(name = "skill")
    val skill: String? = null,
)

@Entity
@Table(name = "cv_certification")
data class CvCertificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "cv_id",
        nullable = false
    )
    val cvId: Long,

    val name: String? = null,
    val year: Int? = null,
)

@Entity
@Table(name = "cv_course")
data class CvCourseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "cv_id",
        nullable = false
    )
    val cvId: Long,

    val name: String? = null,
    val organizer: String? = null,
    val year: Int? = null,
)

@Entity
@Table(name = "cv_language")
data class CvLanguageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "cv_id",
        nullable = false
    )
    val cvId: Long,

    val name: String? = null,
    val level: String? = null,
)

@Entity
@Table(name = "cv_skill_category")
data class CvSkillCategoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "cv_id",
        nullable = false
    )
    val cvId: Long,

    val name: String? = null,
)

@Entity
@Table(name = "cv_skill_in_category")
data class CvSkillInCategoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "skill_category_id",
        nullable = false
    )
    val skillCategoryId: Long,

    val name: String? = null,
    @Column(name = "duration_years")
    val durationYears: Int? = null,
)

@Entity
@Table(name = "cv_attachment")
data class CvAttachmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(
        name = "cv_id",
        nullable = false
    )
    val cvId: Long,

    @Column(name = "file_name")
    val fileName: String? = null,
    val url: String? = null,
)
