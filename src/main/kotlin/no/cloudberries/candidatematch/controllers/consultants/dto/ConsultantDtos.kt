package no.cloudberries.candidatematch.controllers.consultants.dto

import java.math.BigDecimal
import java.time.LocalDate

// Consultant

data class ConsultantSummaryDto(
    val id: Long,
    val name: String,
    val userId: String,
    val defaultCvId: String?,
    val activeCvEntityId: Long?
)

// Assignments

data class AssignmentDto(
    val id: Long?,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val allocationPercent: Int,
    val hourlyRate: BigDecimal?,
    val costRate: BigDecimal?,
    val clientProjectRef: String?,
    val billable: Boolean
)

