package no.cloudberries.candidatematch.infrastructure.entities.consultant

import jakarta.persistence.*
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "project_assignment")
class ProjectAssignmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultant_id", nullable = false)
    val consultant: ConsultantEntity,

    @Column(name = "title", nullable = false)
    val title: String,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date")
    val endDate: LocalDate?,

    @Column(name = "allocation_percent", nullable = false)
    val allocationPercent: Int,

    @Column(name = "hourly_rate", precision = 12, scale = 2)
    val hourlyRate: BigDecimal? = null,

    @Column(name = "cost_rate", precision = 12, scale = 2)
    val costRate: BigDecimal? = null,

    @Column(name = "client_project_ref")
    val clientProjectRef: String? = null,

    @Column(name = "billable", nullable = false)
    val billable: Boolean = true,

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

