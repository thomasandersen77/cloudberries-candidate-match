package no.cloudberries.candidatematch.infrastructure.repositories.analytics

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * Repository for aggregating programming language statistics from normalized skill tables.
 *
 * Data source:
 * - consultant_skill (normalized) joined with skill (normalized)
 *
 * Notes:
 * - We intentionally rely on consultant_skill for counting consultants-per-language to avoid
 *   double-counting that might occur when also mining cv_skill_in_category.
 * - Duration is taken from consultant_skill.duration_years when present; nulls are treated as 0.
 */
@Repository
class ProgrammingLanguageStatsRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    // Per-consultant rows for merging across sources
    data class ConsultantSkillRow(
        val rawSkillUpper: String,
        val consultantId: Long,
        val durationYears: Int, // use 0 when unknown
    )

    // Normalized consultant_skill + skill
    fun fetchNormalizedConsultantSkills(rawUpperNames: Set<String>): List<ConsultantSkillRow> {
        if (rawUpperNames.isEmpty()) return emptyList()
        val placeholders = rawUpperNames.joinToString(",") { "?" }
        val sql = """
            SELECT UPPER(s.name) AS skill_name,
                   cs.consultant_id AS consultant_id,
                   COALESCE(cs.duration_years, 0) AS duration_years
            FROM consultant_skill cs
            JOIN skill s ON s.id = cs.skill_id
            WHERE UPPER(s.name) IN ($placeholders)
        """.trimIndent()
        return jdbcTemplate.query(sql, { rs, _ ->
            ConsultantSkillRow(
                rawSkillUpper = rs.getString("skill_name").trim().uppercase(),
                consultantId = rs.getLong("consultant_id"),
                durationYears = rs.getInt("duration_years")
            )
        }, *rawUpperNames.toTypedArray())
    }

    // String-based cv_skill_in_category -> consultant
    fun fetchFromCvSkillInCategory(rawUpperNames: Set<String>): List<ConsultantSkillRow> {
        if (rawUpperNames.isEmpty()) return emptyList()
        val placeholders = rawUpperNames.joinToString(",") { "?" }
        val sql = """
            SELECT UPPER(csic.name) AS skill_name,
                   c.id AS consultant_id,
                   COALESCE(csic.duration_years, 0) AS duration_years
            FROM cv_skill_in_category csic
            JOIN cv_skill_category csc ON csic.skill_category_id = csc.id
            JOIN consultant_cv cc ON csc.cv_id = cc.id
            JOIN consultant c ON c.id = cc.consultant_id
            WHERE UPPER(csic.name) IN ($placeholders)
        """.trimIndent()
        return jdbcTemplate.query(sql, { rs, _ ->
            ConsultantSkillRow(
                rawSkillUpper = rs.getString("skill_name").trim().uppercase(),
                consultantId = rs.getLong("consultant_id"),
                durationYears = rs.getInt("duration_years")
            )
        }, *rawUpperNames.toTypedArray())
    }

    // String-based cv_project_experience_skill -> consultant (no duration available)
    fun fetchFromCvProjectExperienceSkill(rawUpperNames: Set<String>): List<ConsultantSkillRow> {
        if (rawUpperNames.isEmpty()) return emptyList()
        val placeholders = rawUpperNames.joinToString(",") { "?" }
        val sql = """
            SELECT DISTINCT UPPER(TRIM(cps.skill)) AS skill_name,
                            c.id AS consultant_id
            FROM cv_project_experience_skill cps
            JOIN cv_project_experience pe ON pe.id = cps.project_experience_id
            JOIN consultant_cv cc ON pe.cv_id = cc.id
            JOIN consultant c ON c.id = cc.consultant_id
            WHERE cps.skill IS NOT NULL
              AND TRIM(cps.skill) <> ''
              AND UPPER(TRIM(cps.skill)) IN ($placeholders)
        """.trimIndent()
        return jdbcTemplate.query(sql, { rs, _ ->
            ConsultantSkillRow(
                rawSkillUpper = rs.getString("skill_name").trim().uppercase(),
                consultantId = rs.getLong("consultant_id"),
                durationYears = 0 // unknown in this table
            )
        }, *rawUpperNames.toTypedArray())
    }

    fun countTotalConsultants(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consultant", Int::class.java) ?: 0
}
