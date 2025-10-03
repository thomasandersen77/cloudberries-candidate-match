package no.cloudberries.candidatematch.infrastructure.services

import no.cloudberries.candidatematch.domain.candidate.*
import no.cloudberries.candidatematch.infrastructure.entities.*
import no.cloudberries.candidatematch.infrastructure.repositories.*
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvProjectExperienceSkillRepository
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SkillServiceImpl(
    private val skillRepository: SkillRepository,
    private val consultantSkillRepository: ConsultantSkillRepository,
    private val cvProjectSkillRepository: CvProjectSkillRepository,
    private val consultantRepository: ConsultantRepository,
    private val cvProjectExperienceSkillRepository: CvProjectExperienceSkillRepository,
) : SkillService {

    override fun ensureSkillExists(skillName: String): Long {
        val trimmedName = skillName.trim()
        val existing = skillRepository.findByNameIgnoreCase(trimmedName)
        return existing?.id ?: skillRepository.save(SkillEntity(name = trimmedName)).id!!
    }

    @Transactional(readOnly = true)
    @Timed
    override fun getConsultantSkills(consultantId: Long): List<Skill> {
        val skillEntities = consultantSkillRepository.findByConsultantId(consultantId)
        val skillIds = skillEntities.map { it.skillId }
        val skillNameMap = skillRepository.findAllById(skillIds).associate { it.id!! to it.name }
        
        return skillEntities.map { skillEntity ->
            Skill(
                name = skillNameMap[skillEntity.skillId] ?: "Unknown",
                durationInYears = skillEntity.durationYears,
            )
        }
    }

    override fun updateConsultantSkills(consultantId: Long, skills: List<Skill>) {
        // Remove existing skills
        consultantSkillRepository.deleteByConsultantId(consultantId)
        
        // Add new skills
        skills.forEach { skill ->
            val skillId = ensureSkillExists(skill.name)
            consultantSkillRepository.save(
                ConsultantSkillEntity(
                    consultantId = consultantId,
                    skillId = skillId,
                    durationYears = skill.durationInYears,
                )
            )
        }
    }

    @Transactional(readOnly = true)
    override fun getProjectSkills(projectExperienceId: Long): List<Skill> {
        // First try normalized v2 table (cv_project_experience_skill_v2)
        val projectSkillEntities = cvProjectSkillRepository.findByProjectExperienceId(projectExperienceId)
        if (projectSkillEntities.isNotEmpty()) {
            val skillIds = projectSkillEntities.map { it.skillId }
            val skillNameMap = skillRepository.findAllById(skillIds).associate { it.id!! to it.name }

            return projectSkillEntities.map { projectSkill ->
                Skill(
                    name = skillNameMap[projectSkill.skillId] ?: "Unknown",
                    durationInYears = projectSkill.durationYears
                )
            }
        }

        // Fallback to legacy string-based table (cv_project_experience_skill)
        val legacyRows = cvProjectExperienceSkillRepository
            .findByProjectExperienceIdIn(listOf(projectExperienceId))
        if (legacyRows.isEmpty()) return emptyList()

        return legacyRows.mapNotNull { legacy ->
            val raw = legacy.skill?.trim()
            if (raw.isNullOrBlank()) null else Skill(name = raw, durationInYears = null)
        }
    }

    override fun updateProjectSkills(projectExperienceId: Long, skills: List<Skill>) {
        // Remove existing skills
        cvProjectSkillRepository.deleteByProjectExperienceId(projectExperienceId)
        
        // Add new skills
        skills.forEach { skill ->
            val skillId = ensureSkillExists(skill.name)
            cvProjectSkillRepository.save(
                CvProjectSkillEntity(
                    projectExperienceId = projectExperienceId,
                    skillId = skillId,
                    durationYears = skill.durationInYears
                )
            )
        }
    }

    @Transactional(readOnly = true)
    override fun aggregateSkillsAcrossConsultants(skillFilters: List<String>?): List<SkillAggregate> {
        val consultantSkills = if (skillFilters.isNullOrEmpty()) {
            consultantSkillRepository.findAll()
        } else {
            val skillEntities = skillRepository.findByNameIn(skillFilters)
            val skillIds = skillEntities.mapNotNull { it.id }
            consultantSkillRepository.findAll().filter { it.skillId in skillIds }
        }
        
        val consultantIds = consultantSkills.map { it.consultantId }.distinct()
        val consultants = consultantRepository.findAllById(consultantIds)
            .associateBy { it.id!! }
        
        val skillIds = consultantSkills.map { it.skillId }.distinct()
        val skills = skillRepository.findAllById(skillIds).associateBy { it.id!! }
        
        return consultantSkills
            .groupBy { it.skillId }
            .mapNotNull { (skillId, skillEntities) ->
                val skill = skills[skillId] ?: return@mapNotNull null
                
                val consultantInfos = skillEntities.mapNotNull { skillEntity ->
                    val consultant = consultants[skillEntity.consultantId] ?: return@mapNotNull null
                    ConsultantSkillInfo(
                        userId = consultant.userId,
                        name = consultant.name,
                        cvId = consultant.cvId,
                        durationYears = skillEntity.durationYears,
                    )
                }
                
                SkillAggregate(
                    skillName = skill.name,
                    consultantCount = consultantInfos.size,
                    consultants = consultantInfos
                )
            }
            .sortedBy { it.skillName }
    }
}