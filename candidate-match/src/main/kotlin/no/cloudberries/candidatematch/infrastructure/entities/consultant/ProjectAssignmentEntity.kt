package no.cloudberries.candidatematch.infrastructure.entities.consultant

import jakarta.persistence.*
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "project_assignment")
data class ProjectAssignmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "consultant_id",
        nullable = false
    )
    val consultant: ConsultantEntity,

    @Column(nullable = false)
    val title: String,

    @Column(
        name = "start_date",
        nullable = false
    )
    val startDate: LocalDate,

    @Column(
        name = "end_date",
        nullable = false
    )
    val endDate: LocalDate,

    @Column(
        name = "allocation_percent",
        nullable = false
    )
    val allocationPercent: Int,

    @Column(
        name = "hourly_rate",
        precision = 12,
        scale = 2,
        nullable = false
    )
    val hourlyRate: BigDecimal,

    @Column(
        name = "cost_rate",
        precision = 12,
        scale = 2,
        nullable = false
    )
    val costRate: BigDecimal,

    @Column(name = "client_project_ref")
    val clientProjectRef: String? = null,

    @Column(nullable = false)
    val billable: Boolean = true,
)
