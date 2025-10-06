package no.cloudberries.candidatematch.controllers.projectrequest

import no.cloudberries.candidatematch.domain.AISuggestion
import no.cloudberries.candidatematch.domain.ProjectRequest
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.ProjectRequestRequirementEntity
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.RequirementPriority
import no.cloudberries.candidatematch.service.projectrequest.ProjectRequestAnalysisService

// Legacy mappings
fun ProjectRequestAnalysisService.Aggregate.toDto(): ProjectRequestResponseDto {
    val must = this.requirements
        .filter { it.priority == RequirementPriority.MUST }
        .map { it.toDto() }
    val should = this.requirements
        .filter { it.priority == RequirementPriority.SHOULD }
        .map { it.toDto() }
    return ProjectRequestResponseDto(
        id = this.request.id ?: -1,
        customerName = this.request.customerName,
        title = this.request.title,
        summary = this.request.summary,
        mustRequirements = must,
        shouldRequirements = should,
        originalFilename = this.request.originalFilename,
        uploadedAt = this.request.uploadedAt,
        deadlineDate = this.request.deadlineDate,
    )
}

fun ProjectRequestRequirementEntity.toDto(): ProjectRequirementDto =
    ProjectRequirementDto(name = this.name, details = this.details)

// New mappings for ProjectRequest domain
fun ProjectRequest.toDto(): ProjectRequestDto {
    return ProjectRequestDto(
        id = id?.value,
        customerId = customerId?.value,
        customerName = customerName,
        requiredSkills = requiredSkills.map { it.name },
        startDate = startDate,
        endDate = endDate,
        responseDeadline = responseDeadline,
        status = status,
        requestDescription = requestDescription,
        responsibleSalespersonEmail = responsibleSalespersonEmail,
        aiSuggestions = aISuggestions.map { it.toDto() }
    )
}

fun AISuggestion.toDto(): AISuggestionDto {
    return AISuggestionDto(
        id = id,
        consultantName = consultantName,
        userId = userId,
        cvId = cvId,
        matchScore = matchScore,
        justification = justification,
        createdAt = createdAt,
        skills = skills
    )
}
