package no.cloudberries.candidatematch.service.skills

import mu.KotlinLogging
import no.cloudberries.candidatematch.controllers.consultants.ConsultantSummaryDto
import no.cloudberries.candidatematch.infrastructure.adapters.toDomain
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SkillsService(
    private val consultantRepository: ConsultantRepository,
) {
    private val logger = KotlinLogging.logger { }

    data class SkillAggregate(
        val name: String,
        val konsulenter: List<ConsultantSummaryDto>,
    )

    @Transactional(readOnly = true)
    fun listSkills(skillFilters: List<String>?): List<SkillAggregate> {
        val filters = skillFilters?.map { it.trim().uppercase() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

        val allConsultants = consultantRepository.findAllWithSkills().map { it.toDomain() }

        // Build a map of consultant summaries upfront
        val summariesByUserId: Map<String, ConsultantSummaryDto> = allConsultants.associate { c ->
            val born = c.personalInfo.birthYear?.value ?: 0
            c.id to ConsultantSummaryDto(
                userId = c.id,
                name = c.personalInfo.name,
                email = c.personalInfo.email,
                bornYear = born,
                defaultCvId = c.defaultCvId
            )
        }

        // Flatten (skillName -> userId)
        val skillToUsers = mutableMapOf<String, MutableSet<String>>()
        allConsultants.forEach { c ->
            c.skills.forEach { s ->
                val key = s.name.trim().uppercase()
                if (filters.isEmpty() || key in filters) {
                    val set = skillToUsers.getOrPut(key) { mutableSetOf() }
                    set.add(c.id)
                }
            }
        }

        val aggregates = skillToUsers.toSortedMap().map { (skill, userIds) ->
            val consultants = userIds.mapNotNull { summariesByUserId[it] }
                .sortedBy { it.name.lowercase() }
            SkillAggregate(
                name = skill,
                konsulenter = consultants,
            )
        }

        logger.info { "Computed ${aggregates.size} skill aggregates${if (filters.isNotEmpty()) " with filters=" + filters.joinToString(",") else ""}" }
        return aggregates
    }
}