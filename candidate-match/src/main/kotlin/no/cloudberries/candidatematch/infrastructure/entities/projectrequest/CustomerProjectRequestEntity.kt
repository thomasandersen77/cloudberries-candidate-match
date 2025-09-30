package no.cloudberries.candidatematch.infrastructure.entities.projectrequest

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "customer_project_request")
data class CustomerProjectRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "customer_name")
    val customerName: String? = null,

    @Column(name = "title")
    val title: String? = null,

    @Column(name = "summary", columnDefinition = "text")
    val summary: String? = null,

    @Column(name = "original_filename")
    val originalFilename: String? = null,

    @Column(name = "original_text", columnDefinition = "text")
    val originalText: String? = null,

    @OneToMany(mappedBy = "projectRequest", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val requirements: List<ProjectRequestRequirementEntity> = emptyList(),
)
