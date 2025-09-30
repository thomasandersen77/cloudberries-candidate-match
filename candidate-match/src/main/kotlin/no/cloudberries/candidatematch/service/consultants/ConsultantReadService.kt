package no.cloudberries.candidatematch.service.consultants

import no.cloudberries.candidatematch.dto.consultants.ConsultantSummaryDto
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ConsultantReadService(
    private val flowcaseHttpClient: FlowcaseHttpClient,
) {
    fun listConsultants(name: String?, pageable: Pageable): Page<ConsultantSummaryDto> {
        val all = flowcaseHttpClient.fetchAllUsers().flowcaseUserDTOs

        val filtered = all.filter {
            name == null || it.name.contains(
                name,
                ignoreCase = true
            )
        }

        val totalElements = filtered.size.toLong()
        val pageSize = pageable.pageSize
        val pageNumber = pageable.pageNumber

        val pageOfConsultants = if (filtered.isEmpty()) {
            emptyList()
        } else {
            filtered.chunked(pageSize).getOrNull(pageNumber) ?: emptyList()
        }

        val mapped = pageOfConsultants.map {
            ConsultantSummaryDto(
                userId = it.userId,
                name = it.name,
                email = it.email,
                bornYear = it.bornYear,
                defaultCvId = it.cvId
            )
        }

        return PageImpl(
            mapped,
            pageable,
            totalElements
        )
    }
}