package no.cloudberries.candidatematch.service.consultants

import no.cloudberries.candidatematch.controllers.consultants.ConsultantSummaryDto
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ConsultantReadService(
    private val flowcaseHttpClient: FlowcaseHttpClient,
) {
    data class PageResult<T>(val items: List<T>, val pageable: Pageable, val total: Long)

    fun listConsultants(name: String?, page: Int, size: Int): PageResult<ConsultantSummaryDto> {
        val pageable = PageRequest.of(
            page,
            size
        )
        val all = flowcaseHttpClient.fetchAllUsers().flowcaseUserDTOs
        val filtered = if (name.isNullOrBlank()) all else all.filter {
            it.name.contains(
                name,
                ignoreCase = true
            )
        }
        val start = page * size
        val end = (start + size).coerceAtMost(filtered.size)
        val pageItems = if (start >= filtered.size) emptyList() else filtered.subList(
            start,
            end
        )
        val mapped = pageItems.map {
            ConsultantSummaryDto(
                userId = it.userId,
                name = it.name,
                email = it.email,
                bornYear = it.bornYear,
                defaultCvId = it.cvId
            )
        }
        return PageResult(
            mapped,
            pageable,
            filtered.size.toLong()
        )
    }
}