package no.cloudberries.candidatematch.domain.candidate

/**
 * Domain service for skill-related operations.
 * Follows DDD principles by keeping business logic in the domain layer.
 */
interface SkillService {
    /**
     * Ensures skill exists in the system, creating if necessary
     */
    fun ensureSkillExists(skillName: String): Long
    
    /**
     * Gets all skills for a consultant
     */
    fun getConsultantSkills(consultantId: Long): List<Skill>
    
    /**
     * Updates all skills for a consultant, replacing existing ones
     */
    fun updateConsultantSkills(consultantId: Long, skills: List<Skill>)
    
    /**
     * Gets all skills used in a project
     */
    fun getProjectSkills(projectExperienceId: Long): List<Skill>
    
    /**
     * Updates all skills for a project, replacing existing ones
     */
    fun updateProjectSkills(projectExperienceId: Long, skills: List<Skill>)
    
    /**
     * Aggregates skills across consultants for reporting
     */
    fun aggregateSkillsAcrossConsultants(skillFilters: List<String>? = null): List<SkillAggregate>
}

/**
 * Aggregated skill information across consultants
 */
data class SkillAggregate(
    val skillName: String,
    val consultantCount: Int,
    val consultants: List<ConsultantSkillInfo>
)

/**
 * Consultant information in skill aggregates
 */
data class ConsultantSkillInfo(
    val userId: String,
    val name: String,
    val cvId: String,
    val durationYears: Int?,
)