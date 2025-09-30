package no.cloudberries.candidatematch.service.skills

import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.candidate.SkillService
import no.cloudberries.candidatematch.dto.consultants.ConsultantSummaryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SkillsService(
    private val skillService: SkillService,
    private val projectSkillFetcher: ProjectSkillFetcher = ProjectSkillFetcher.Noop,
) {
    private val logger = KotlinLogging.logger { }

    data class SkillAggregate(
        val name: String,
        val konsulenter: List<ConsultantSummaryDto>,
    )

    @Transactional(readOnly = true)
    fun listSkills(skillFilters: List<String>?): List<SkillAggregate> {
        val normalizedFilters = skillFilters
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }

        // 1) Aggregates from consultant-level skills via domain service
        val domainAggregates = skillService.aggregateSkillsAcrossConsultants(normalizedFilters)
        val domainBySkillUpper: Map<String, List<ConsultantSummaryDto>> = domainAggregates
            .associate { domainAggregate ->
                val consultants = domainAggregate.consultants.map { consultantSkillInfo ->
                    ConsultantSummaryDto(
                        userId = consultantSkillInfo.userId,
                        name = consultantSkillInfo.name,
                        email = "", // Not available in skill aggregate
                        bornYear = 0, // Not available in skill aggregate
                        defaultCvId = consultantSkillInfo.cvId
                    )
                }.distinctBy { it.userId }
                    .sortedBy { it.name.lowercase() }
                domainAggregate.skillName.uppercase() to consultants
            }

        // 2) Aggregates from project-level skills via fetcher (legacy table)
        val upperFilters: Set<String> = normalizedFilters?.map { it.uppercase() }?.toSet() ?: emptySet()
        val projectRows = projectSkillFetcher.fetch(upperFilters)
        val projectBySkillUpper: Map<String, List<ConsultantSummaryDto>> = projectRows
            .groupBy { it.skillName.uppercase() }
            .mapValues { (_, rows) ->
                rows.map { row ->
                    ConsultantSummaryDto(
                        userId = row.userId,
                        name = row.name,
                        email = "",
                        bornYear = 0,
                        defaultCvId = row.defaultCvId
                    )
                }.distinctBy { it.userId }
                    .sortedBy { it.name.lowercase() }
            }

        // 3) Merge consultant-level and project-level aggregates
        val allSkillKeys = (domainBySkillUpper.keys + projectBySkillUpper.keys).toSortedSet()
        val merged = allSkillKeys.map { key ->
            val consultants = ((domainBySkillUpper[key] ?: emptyList()) + (projectBySkillUpper[key] ?: emptyList()))
                .distinctBy { it.userId }
                .sortedBy { it.name.lowercase() }
            SkillAggregate(name = key, konsulenter = consultants)
        }

        val filterCount = normalizedFilters?.size ?: 0
        logger.info { "Computed ${merged.size} skill aggregates using $filterCount filters (merged consultant + project skills)" }
        return merged
    }
}

interface ProjectSkillFetcher {
    fun fetch(normalizedFilters: Set<String>): List<SkillAggregateRow>

    object Noop : ProjectSkillFetcher {
        override fun fetch(normalizedFilters: Set<String>): List<SkillAggregateRow> = emptyList()
    }
}
