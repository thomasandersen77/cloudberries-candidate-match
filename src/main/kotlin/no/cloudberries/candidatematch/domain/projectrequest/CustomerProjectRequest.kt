package no.cloudberries.candidatematch.domain.projectrequest

import no.cloudberries.candidatematch.infrastructure.entities.CustomerProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.entities.ProjectRequestRequirementEntity
import no.cloudberries.candidatematch.infrastructure.entities.RequirementPriority
import no.cloudberries.candidatematch.infrastructure.entities.fromDomain

/**
 * Domain objects for persisted Customer Project Request
 */

data class CustomerProjectRequestId(val value: Long)

data class CustomerProjectRequest(
    val id: CustomerProjectRequestId? = null,
    val customerName: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val originalFilename: String? = null,
    val originalText: String? = null,
    val requirements: List<CustomerProjectRequirement> = emptyList(),
)

data class CustomerProjectRequirement(
    val id: Long? = null,
    val name: String,
    val details: String? = null,
    val priority: RequirementPriority,
)

// Mapping

fun CustomerProjectRequest.toEntity(): CustomerProjectRequestEntity = CustomerProjectRequestEntity(
    id = this.id?.value,
    customerName = this.customerName,
    title = this.title,
    summary = this.summary,
    originalFilename = this.originalFilename,
    originalText = this.originalText,
    requirements = this.requirements.map { it.fromDomain() }
)

fun CustomerProjectRequirement.fromDomain(): ProjectRequestRequirementEntity = ProjectRequestRequirementEntity(
    id = this.id,
    name = this.name,
    details = this.details,
    priority = this.priority,
)