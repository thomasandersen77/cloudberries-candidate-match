package no.cloudberries.candidatematch.infrastructure.entities

import jakarta.persistence.*
import no.cloudberries.candidatematch.domain.projectrequest.CustomerProjectRequest
import no.cloudberries.candidatematch.domain.projectrequest.CustomerProjectRequestId
import no.cloudberries.candidatematch.domain.projectrequest.CustomerProjectRequirement

@Entity
@Table(name = "customer_project_request")
data class CustomerProjectRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "customer_name")
    val customerName: String? = null,
    val title: String? = null,
    @Column(columnDefinition = "text")
    val summary: String? = null,
    @Column(name = "original_filename")
    val originalFilename: String? = null,
    @Column(
        name = "original_text",
        columnDefinition = "text"
    )
    val originalText: String? = null,
    @OneToMany(
        mappedBy = "projectRequest",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    val requirements: List<ProjectRequestRequirementEntity> = emptyList(),
)

@Entity
@Table(name = "project_request_requirement")
data class ProjectRequestRequirementEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val name: String,
    @Column(columnDefinition = "text")
    val details: String? = null,
    @Enumerated(EnumType.STRING)
    val priority: RequirementPriority,
) {
    // JPA requires a no-arg constructor for certain collection loading paths
    constructor() : this(
        id = null,
        name = "",
        details = null,
        priority = RequirementPriority.MUST
    )

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "project_request_id",
        nullable = false
    )
    var projectRequest: CustomerProjectRequestEntity? = null
}

enum class RequirementPriority { MUST, SHOULD }

// Mapping

fun CustomerProjectRequestEntity.toDomain(): CustomerProjectRequest = CustomerProjectRequest(
    id = this.id?.let { CustomerProjectRequestId(it) },
    customerName = this.customerName,
    title = this.title,
    summary = this.summary,
    originalFilename = this.originalFilename,
    originalText = this.originalText,
    requirements = this.requirements.map { it.toDomain() },
)

fun ProjectRequestRequirementEntity.toDomain(): CustomerProjectRequirement = CustomerProjectRequirement(
    id = this.id,
    name = this.name,
    details = this.details,
    priority = this.priority,
)

// Reverse mapping used by domain extension
fun CustomerProjectRequirement.fromDomain(): ProjectRequestRequirementEntity = ProjectRequestRequirementEntity(
    id = this.id,
    name = this.name,
    details = this.details,
    priority = this.priority,
)

fun CustomerProjectRequest.fromDomain(): CustomerProjectRequestEntity {
    val entity = CustomerProjectRequestEntity(
        id = this.id?.value,
        customerName = this.customerName,
        title = this.title,
        summary = this.summary,
        originalFilename = this.originalFilename,
        originalText = this.originalText,
        requirements = this.requirements.map { it.fromDomain() }
    )
    entity.requirements.forEach { it.projectRequest = entity }
    return entity
}
