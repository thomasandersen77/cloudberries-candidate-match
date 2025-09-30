package no.cloudberries.candidatematch.controllers.projectrequest

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.infrastructure.entities.RequestStatus
import java.time.LocalDateTime

// DTOs used by ProjectRequest endpoints

data class ProjectRequestResponseDto(
    val id: Long,
    val customerName: String?,
    val title: String?,
    val summary: String?,
    val mustRequirements: List<ProjectRequirementDto>,
    val shouldRequirements: List<ProjectRequirementDto>,
    val originalFilename: String?,
)

data class ProjectRequirementDto(
    val name: String,
    val details: String?
)

data class PagedProjectRequestResponseDto(
    val content: List<ProjectRequestResponseDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

data class CreateProjectRequestDto(
    @field:NotBlank(message = "Customer name is required")
    val customerName: String,

    @field:NotNull(message = "Required skills are required")
    var requiredSkills: List<Skill>,

    @field:NotNull(message = "Start date is required")
    var startDate: LocalDateTime,

    @field:NotNull(message = "End date is required")
    var endDate: LocalDateTime,

    @field:NotNull(message = "Response deadline is required")
    var responseDeadline: LocalDateTime,

    val status: RequestStatus? = null,

    @field:NotBlank(message = "Request description is required")
    var requestDescription: String,

    @field:NotBlank(message = "Responsible salesperson email is required")
    @field:Email(message = "varid email is required")
    val responsibleSalespersonEmail: String
)

data class ProjectRequestDto(
    val id: Long?,
    val customerId: Long?,
    val customerName: String,
    val requiredSkills: List<String>,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val responseDeadline: LocalDateTime,
    val status: RequestStatus,
    val requestDescription: String,
    val responsibleSalespersonEmail: String,
    val aiSuggestions: List<AISuggestionDto>
)

data class AISuggestionDto(
    val id: Long?,
    val consultantName: String,
    val userId: String,
    val cvId: String,
    val matchScore: Double,
    val justification: String,
    val createdAt: LocalDateTime,
    val skills: List<String>
)
