package no.cloudberries.candidatematch.service.consultants

import no.cloudberries.candidatematch.controllers.consultants.*
import no.cloudberries.candidatematch.domain.candidate.SkillService
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantFlatView
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Simplified service that orchestrates consultant CV operations.
 * Follows SRP by delegating specific concerns to focused services.
 */
@Service
class ConsultantWithCvService(
    private val consultantRepository: ConsultantRepository,
    private val cvDataAggregationService: CvDataAggregationService,
    private val skillService: SkillService
) {

    fun getAllConsultantsWithCvs(onlyActiveCv: Boolean = false): List<ConsultantWithCvDto> {
        val consultantFlats = consultantRepository.findAllFlat()
        if (consultantFlats.isEmpty()) return emptyList()
        
        return buildConsultantDtos(consultantFlats, onlyActiveCv)
    }

    fun getConsultantsWithCvsPaged(pageable: Pageable, onlyActiveCv: Boolean = false): Page<ConsultantWithCvDto> {
        val page = consultantRepository.findAllFlat(pageable)
        if (page.isEmpty) {
            return PageImpl(emptyList(), pageable, 0)
        }
        
        val dtos = buildConsultantDtos(page.content, onlyActiveCv)
        return PageImpl(dtos, pageable, page.totalElements)
    }

    /**
     * Returns the list of CVs for a given consultant userId.
     */
    @Timed
    fun getCvsByUserId(userId: String, onlyActiveCv: Boolean = false): List<ConsultantCvDto> {
        val consultant = consultantRepository.findByUserId(userId) ?: return emptyList()
        val result = cvDataAggregationService.aggregateCvData(listOf(consultant.id!!), onlyActiveCv)
        return result[consultant.id] ?: emptyList()
    }

    /**
     * Finds top consultants based on skill matching for AI analysis.
     * Takes top 3 skills by frequency in project experiences.
     */
    @Timed
    @Transactional(readOnly = true)
    fun getTopConsultantsBySkills(skills: List<String>, limit: Int = 20): List<ConsultantWithCvDto> {
        if (skills.isEmpty()) return emptyList()
        
        val consultantFlats = consultantRepository.findAllFlat()
        if (consultantFlats.isEmpty()) return emptyList()
        
        val allConsultants = buildConsultantDtos(consultantFlats, onlyActiveCv = true)
        
        // Score consultants by skill overlap
        val scoredConsultants = allConsultants.map { consultant ->
            val skillSet = consultant.skills.map { it.uppercase() }.toSet()
            val requiredSkillSet = skills.map { it.uppercase() }.toSet()
            
            // Calculate overlap score
            val overlapCount = skillSet.intersect(requiredSkillSet).size
            val overlapRatio = if (requiredSkillSet.isNotEmpty()) overlapCount.toDouble() / requiredSkillSet.size else 0.0
            
            // Bonus for having more relevant skills than required
            val relevantSkillsCount = skillSet.intersect(requiredSkillSet).size
            val bonus = if (relevantSkillsCount > 0) relevantSkillsCount * 0.1 else 0.0
            
            val totalScore = overlapRatio + bonus
            
            Pair(consultant, totalScore)
        }
        
        return scoredConsultants
            .filter { it.second > 0.0 } // Only include consultants with some skill overlap
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun buildConsultantDtos(
        consultantFlats: List<ConsultantFlatView>,
        onlyActiveCv: Boolean
    ): List<ConsultantWithCvDto> {
        val consultantIds = consultantFlats.map { it.getId() }
        
        // Delegate CV aggregation to specialized service
        val cvDataByConsultant = cvDataAggregationService.aggregateCvData(consultantIds, onlyActiveCv)
        
        // Load consultant skills using domain service and compute top-3 using combined heuristic:
        // score = 3 * (frequency in project experiences) + (duration in years)
        val skillsByConsultant = consultantIds.associateWith { consultantId ->
            val domainSkills = skillService.getConsultantSkills(consultantId)
            val durationByName = domainSkills.associate { it.name to (it.durationInYears ?: 0) }
            
            val projectFreq = mutableMapOf<String, Int>()
            val consultantCvs = cvDataByConsultant[consultantId] ?: emptyList()
            consultantCvs.forEach { cv ->
                cv.projectExperience.forEach { pe ->
                    pe.skills.forEach { raw ->
                        val key = raw.trim()
                        if (key.isNotEmpty()) projectFreq[key] = (projectFreq[key] ?: 0) + 1
                    }
                }
            }
            
            val allSkillNames = (durationByName.keys + projectFreq.keys).toSet()
            val scored = allSkillNames.map { name ->
                val score = 3 * (projectFreq[name] ?: 0) + (durationByName[name] ?: 0)
                val duration = durationByName[name] ?: 0
                Triple(name, score, duration)
            }
            scored.sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }
                .thenByDescending { it.third }
                .thenBy { it.first.lowercase() })
                .take(3)
                .map { it.first }
        }
        
        return consultantFlats.map { consultant ->
            ConsultantWithCvDto(
                id = consultant.getId(),
                userId = consultant.getUserId(),
                name = consultant.getName(),
                cvId = consultant.getCvId(),
                skills = skillsByConsultant[consultant.getId()] ?: emptyList(),
                cvs = cvDataByConsultant[consultant.getId()] ?: emptyList()
            )
        }
    }
}
