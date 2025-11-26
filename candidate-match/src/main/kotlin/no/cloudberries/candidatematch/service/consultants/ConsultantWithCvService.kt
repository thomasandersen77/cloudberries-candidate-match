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
     * Extracts skill keywords from verbose requirement descriptions to match against consultant skills.
     * 
     * For example: "Minst 12 mnd erfaring med Java/Kotlin" -> extracts ["Java", "Kotlin"]
     */
    @Timed
    @Transactional(readOnly = true)
    fun getTopConsultantsBySkills(skills: List<String>, limit: Int = 20): List<ConsultantWithCvDto> {
        if (skills.isEmpty()) return emptyList()
        
        val consultantFlats = consultantRepository.findAllFlat()
        if (consultantFlats.isEmpty()) return emptyList()
        
        val allConsultants = buildConsultantDtos(consultantFlats, onlyActiveCv = true)
        
        // Extract skill keywords from verbose requirement descriptions
        val extractedSkills = extractSkillKeywords(skills)
        
        // If no skills could be extracted, return top consultants by CV quality
        if (extractedSkills.isEmpty()) {
            return allConsultants.take(limit)
        }
        
        // Score consultants by skill overlap
        val scoredConsultants = allConsultants.map { consultant ->
            val consultantSkillSet = consultant.skills.map { it.uppercase() }.toSet()
            val requiredSkillSet = extractedSkills.map { it.uppercase() }.toSet()
            
            // Use partial matching: consultant skill contains or is contained in required skill
            var matchCount = 0
            for (consultantSkill in consultantSkillSet) {
                for (requiredSkill in requiredSkillSet) {
                    if (consultantSkill.contains(requiredSkill) || requiredSkill.contains(consultantSkill)) {
                        matchCount++
                        break // Count each consultant skill once
                    }
                }
            }
            
            // Calculate score based on match count and CV quality
            val matchRatio = if (requiredSkillSet.isNotEmpty()) matchCount.toDouble() / requiredSkillSet.size else 0.0
            val cvQualityBonus = if (consultant.cvs.isNotEmpty()) 0.1 else 0.0
            
            val totalScore = matchRatio + cvQualityBonus
            
            Pair(consultant, totalScore)
        }
        
        return scoredConsultants
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
    
    /**
     * Extracts skill keywords from verbose requirement descriptions.
     * 
     * Common patterns:
     * - "erfaring med X" -> X
     * - "X/Y/Z" -> [X, Y, Z]
     * - "X og Y" -> [X, Y]
     * - Technical terms (React, TypeScript, Java, Kotlin, SQL, etc.)
     */
    private fun extractSkillKeywords(requirements: List<String>): List<String> {
        val keywords = mutableSetOf<String>()
        
        // Common technology keywords to look for
        val techKeywords = setOf(
            "Java", "Kotlin", "React", "TypeScript", "JavaScript", "CSS", "HTML",
            "SQL", "PostgreSQL", "MySQL", "MongoDB",
            "Docker", "Kubernetes", "REST", "JSON", "XML",
            "Spring", "Spring Boot", "Microservices", "Material UI", "Material-UI",
            "Git", "CI/CD", "DevOps", "Agile", "Scrum",
            "Domain Driven Design", "DDD", "Test Driven Development", "TDD",
            "Node.js", "Express", "Angular", "Vue", "Python", "C#", ".NET",
            "AWS", "Azure", "GCP", "Cloud"
        )
        
        requirements.forEach { requirement ->
            val upperReq = requirement.uppercase()
            
            // Check for each tech keyword
            techKeywords.forEach { tech ->
                if (upperReq.contains(tech.uppercase())) {
                    keywords.add(tech)
                }
            }
            
            // Extract slash-separated skills: "Java/Kotlin" -> ["Java", "Kotlin"]
            val slashPattern = """([A-Z][a-z]+(?:[A-Z][a-z]+)*)/([A-Z][a-z]+(?:[A-Z][a-z]+)*)""".toRegex()
            slashPattern.findAll(requirement).forEach { match ->
                keywords.add(match.groupValues[1])
                keywords.add(match.groupValues[2])
            }
        }
        
        return keywords.toList()
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
