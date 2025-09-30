package no.cloudberries.candidatematch.service.skills

/**
 * Port for reading consultant skill aggregates from the persistence layer.
 * Implemented by infrastructure repositories.
 * Updated to work with normalized skill names instead of enums.
 */
interface ConsultantSkillReader {
    /**
     * Return rows of (skill, consultant-summary fields) for consultants having at least one of the given skills.
     * If skillFilters is null or empty, implementors may return all rows (same as findAllSkillAggregates).
     */
    fun findSkillAggregates(skillFilters: Collection<String>?): List<SkillAggregateRow>

    /**
     * Return rows of (skill, consultant-summary fields) for all consultant-skill associations.
     */
    fun findAllSkillAggregates(): List<SkillAggregateRow>
}

/**
 * Lightweight row returned from the repository, optimized for skill aggregation.
 * Email and birthYear are optional and may be null depending on the query capabilities.
 */
data class SkillAggregateRow(
    val skillName: String,
    val userId: String,
    val name: String,
    val defaultCvId: String,
)