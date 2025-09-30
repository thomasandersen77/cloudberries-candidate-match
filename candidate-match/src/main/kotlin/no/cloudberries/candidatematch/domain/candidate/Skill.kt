package no.cloudberries.candidatematch.domain.candidate

import jakarta.persistence.Embeddable

/**
 * Represents a skill with flexible attributes.
 * Replaces the enum-based approach to allow dynamic skill management.
 */
@Embeddable
data class Skill(
    val name: String,
    val durationInYears: Int? = null,
) {
    init {
        require(name.isNotBlank()) { "Skill name cannot be blank" }
        val dur = durationInYears
        require(dur == null || dur >= 0) {
            "Duration in years must be non-negative"
        }
    }

    companion object {
        /**
         * Creates a basic skill with just a name
         */
        fun of(name: String): Skill = Skill(name = name.trim())
        
        /**
         * Creates a skill with duration
         */
        fun withDuration(name: String, durationInYears: Int): Skill = 
            Skill(name = name.trim(), durationInYears = durationInYears)
            
        /**
         * Creates a skill acquired in a specific project
         */
        fun fromProject(name: String, projectName: String, durationInYears: Int? = null): Skill =
            Skill(name = name.trim(), durationInYears = durationInYears)
    }
}

/**
 * Legacy enum values for backward compatibility during migration
 * TODO: Remove after migration is complete
 */
@Deprecated("Use Skill data class instead")
enum class LegacySkill(val displayName: String) {
    JAVASCRIPT("JavaScript"),
    JAVA("Java"),
    KOTLIN("Kotlin"),
    PYTHON("Python"),
    REACT("React"),
    REACT_NATIVE("React Native"),
    ANGULAR("Angular"),
    GIT("Git"),
    SQL("SQL"),
    MSSQL("MS SQL"),
    POSTGRESQL("PostgreSQL"),
    NO_SQL("NoSQL"),
    CASSANDRA("Cassandra"),
    SPRING("Spring"),
    BACKEND("Backend");
    
    fun toSkill(): Skill = Skill.of(displayName)
}
