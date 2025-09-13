package no.cloudberries.candidatematch.infrastructure.entities.consultant

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Embeddable
class YearMonthPeriodEmbeddable(
    @Column(name = "from_year")
    val fromYear: Int?,
    @Column(name = "from_month")
    val fromMonth: Int?,
    @Column(name = "to_year")
    val toYear: Int?,
    @Column(name = "to_month")
    val toMonth: Int?,
)


@Entity
@Table(name = "consultant_cv")
class ConsultantCvEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultant_id", nullable = false)
    val consultant: no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity,

    @Column(name = "version_tag", nullable = false)
    val versionTag: String,

    @Column(name = "active", nullable = false)
    val active: Boolean = false,

    @Column(name = "quality_score")
    val qualityScore: Int? = null,

    @OneToMany(mappedBy = "cv", cascade = [CascadeType.ALL], orphanRemoval = true)
    val keyQualifications: MutableList<CvKeyQualificationEntity> = mutableListOf(),

    @OneToMany(mappedBy = "cv", cascade = [CascadeType.ALL], orphanRemoval = true)
    val educations: MutableList<CvEducationEntity> = mutableListOf(),

    @OneToMany(mappedBy = "cv", cascade = [CascadeType.ALL], orphanRemoval = true)
    val workExperiences: MutableList<CvWorkExperienceEntity> = mutableListOf(),

    @OneToMany(mappedBy = "cv", cascade = [CascadeType.ALL], orphanRemoval = true)
    val projectExperiences: MutableList<CvProjectExperienceEntity> = mutableListOf(),

    @OneToMany(mappedBy = "cv", cascade = [CascadeType.ALL], orphanRemoval = true)
    val certifications: MutableList<CvCertificationEntity> = mutableListOf(),

    @OneToMany(mappedBy = "cv", cascade = [CascadeType.ALL], orphanRemoval = true)
    val courses: MutableList<CvCourseEntity> = mutableListOf(),

    @OneToMany(mappedBy = "cv", cascade = [CascadeType.ALL], orphanRemoval = true)
    val languages: MutableList<CvLanguageSkillEntity> = mutableListOf(),

    @OneToMany(mappedBy = "cv", cascade = [CascadeType.ALL], orphanRemoval = true)
    val skillCategories: MutableList<CvSkillCategoryEntity> = mutableListOf(),

    @OneToMany(mappedBy = "cv", cascade = [CascadeType.ALL], orphanRemoval = true)
    val attachments: MutableList<CvAttachmentEntity> = mutableListOf(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant? = null,

    @Version
    @Column(name = "version", nullable = false)
    val version: Long? = null,
)

@Entity
@Table(name = "cv_key_qualification")
class CvKeyQualificationEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    val cv: ConsultantCvEntity,
    @Column(nullable = false)
    val label: String,
    @Column(nullable = false, length = 2000)
    val description: String,
)

@Entity
@Table(name = "cv_education")
class CvEducationEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    val cv: ConsultantCvEntity,
    @Column(nullable = false)
    val degree: String,
    @Column(nullable = false)
    val school: String,
    @Embedded
    val period: YearMonthPeriodEmbeddable,
)

@Entity
@Table(name = "cv_work_experience")
class CvWorkExperienceEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    val cv: ConsultantCvEntity,
    @Column(nullable = false)
    val employer: String,
    @Embedded
    val period: YearMonthPeriodEmbeddable,
)

@Entity
@Table(name = "cv_project_experience")
class CvProjectExperienceEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    val cv: ConsultantCvEntity,
    @Column(nullable = false)
    val customer: String,
    @Column(nullable = false)
    val description: String,
    @Column(name = "long_description", length = 4000)
    val longDescription: String,
    @Embedded
    val period: YearMonthPeriodEmbeddable,

    @ElementCollection
    @CollectionTable(name = "cv_project_experience_role", joinColumns = [JoinColumn(name = "project_experience_id")])
    @Column(name = "role_name")
    val roles: MutableSet<String> = mutableSetOf(),

    @ElementCollection
    @CollectionTable(name = "cv_project_experience_skill", joinColumns = [JoinColumn(name = "project_experience_id")])
    @AttributeOverrides(
        AttributeOverride(name = "skillName", column = Column(name = "skill_name")),
        AttributeOverride(name = "durationInYears", column = Column(name = "duration_in_years"))
    )
    val skillsUsed: MutableSet<SkillWithDurationEmbeddable> = mutableSetOf(),
)

@Embeddable
class SkillWithDurationEmbeddable(
    val skillName: String,
    val durationInYears: Int?
)

@Entity
@Table(name = "cv_certification")
class CvCertificationEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    val cv: ConsultantCvEntity,
    @Column(nullable = false)
    val name: String,
    @Column(name = "year")
    val year: Int?,
)

@Entity
@Table(name = "cv_course")
class CvCourseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    val cv: ConsultantCvEntity,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val organizer: String,
    @Column(name = "year")
    val year: Int?,
)

@Entity
@Table(name = "cv_language")
class CvLanguageSkillEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    val cv: ConsultantCvEntity,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val level: String,
)

@Entity
@Table(name = "cv_skill_category")
class CvSkillCategoryEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    val cv: ConsultantCvEntity,
    @Column(name = "category_name", nullable = false)
    val name: String,
    @ElementCollection
    @CollectionTable(name = "cv_skill_in_category", joinColumns = [JoinColumn(name = "skill_category_id")])
    @AttributeOverrides(
        AttributeOverride(name = "skillName", column = Column(name = "skill_name")),
        AttributeOverride(name = "durationInYears", column = Column(name = "duration_in_years"))
    )
    val skills: MutableSet<SkillWithDurationEmbeddable> = mutableSetOf(),
)

@Entity
@Table(name = "cv_attachment")
class CvAttachmentEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    val cv: ConsultantCvEntity,
    @Column(name = "file_url", nullable = false)
    val fileUrl: String,
    @Column(name = "file_type")
    val fileType: String? = null,
    @Column(name = "file_name")
    val fileName: String? = null,
    @Column(name = "size_in_bytes")
    val sizeInBytes: Long? = null,
)

