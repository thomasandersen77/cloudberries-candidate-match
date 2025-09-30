package no.cloudberries.candidatematch.dto.consultants

data class ConsultantSummaryDto(
    val userId: String,
    val name: String,
    val email: String,
    val bornYear: Int,
    val defaultCvId: String,
)