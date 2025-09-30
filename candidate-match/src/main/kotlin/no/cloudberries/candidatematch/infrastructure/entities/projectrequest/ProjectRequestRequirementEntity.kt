package no.cloudberries.candidatematch.infrastructure.entities.projectrequest

import jakarta.persistence.*

@Entity
@Table(name = "project_request_requirement")
data class ProjectRequestRequirementEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_request_id")
    val projectRequest: CustomerProjectRequestEntity? = null,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "details", columnDefinition = "text")
    val details: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    val priority: RequirementPriority = RequirementPriority.MUST,
)
