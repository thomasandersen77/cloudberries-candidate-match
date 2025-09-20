package no.cloudberries.candidatematch.service.consultants

import jakarta.transaction.Transactional
import no.cloudberries.candidatematch.controllers.consultants.ConsultantSummaryDto
import no.cloudberries.candidatematch.infrastructure.adapters.toDomain
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ConsultantReadService(
    private val consultantRepository: ConsultantRepository,
) {
    @Transactional
    fun listConsultants(name: String?, pageable: Pageable): Page<ConsultantSummaryDto> {

        val all = consultantRepository.findAll().map { it.toDomain() }

        val filtered = all.filter {
            name == null || it.personalInfo.name.contains(
                name,
                ignoreCase = true
            )
        }

        val pageOfConsultants = if (filtered.isEmpty()) {
            emptyList()
        } else {
            filtered.chunked(pageable.pageSize).getOrNull(pageable.pageNumber) ?: emptyList()
        }

        val mapped = pageOfConsultants.map {
            ConsultantSummaryDto(
                userId = it.id,
                name = it.personalInfo.name,
                email = it.personalInfo.email,
                bornYear = it.personalInfo.birthYear?.get(java.time.temporal.ChronoField.YEAR)?.toInt() ?: 0,
                defaultCvId = it.defaultCvId
            )
        }

        return PageImpl(
            mapped,
            pageable,
            pageable.pageNumber.toLong() + 1
        )
    }
}