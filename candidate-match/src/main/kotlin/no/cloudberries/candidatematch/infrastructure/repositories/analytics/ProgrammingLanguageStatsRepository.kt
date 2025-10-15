package no.cloudberries.candidatematch.infrastructure.repositories.analytics

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProgrammingLanguageStatsRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    data class ConsultantSkillRow(
        val rawSkillUpper: String,
        val consultantId: Long,
        val durationYears: Int
    )

    fun fetchNormalizedConsultantSkills(upperSynonyms: Set<String>): List<ConsultantSkillRow> {
        if (upperSynonyms.isEmpty()) return emptyList()
        val placeholders = upperSynonyms.joinToString(",") { "?" }
        val sql = """
            SELECT UPPER(TRIM(s.name)) AS skill_upper,
                   c.id AS consultant_id,
                   COALESCE(cs.duration_years, 0) AS duration_years
            FROM consultant_skill cs
            JOIN consultant c ON c.id = cs.consultant_id
            JOIN skill s ON s.id = cs.skill_id
            WHERE UPPER(s.name) IN ($placeholders)
        """.trimIndent()
        val params = upperSynonyms.toTypedArray()
        return jdbcTemplate.query(sql, params) { rs, _ ->
            ConsultantSkillRow(
                rawSkillUpper = rs.getString("skill_upper").trim().uppercase(),
                consultantId = rs.getLong("consultant_id"),
                durationYears = rs.getInt("duration_years")
            )
        }
    }

    fun fetchFromCvSkillInCategory(upperSynonyms: Set<String>): List<ConsultantSkillRow> {
        if (upperSynonyms.isEmpty()) return emptyList()
        val placeholders = upperSynonyms.joinToString(",") { "?" }
        val sql = """
            SELECT UPPER(TRIM(csic.name)) AS skill_upper,
                   c.id AS consultant_id,
                   COALESCE(csic.duration_years, 0) AS duration_years
            FROM cv_skill_in_category csic
            JOIN cv_skill_category csc ON csc.id = csic.skill_category_id
            JOIN consultant_cv cc ON cc.id = csc.cv_id
            JOIN consultant c ON c.id = cc.consultant_id
            WHERE UPPER(csic.name) IN ($placeholders)
        """.trimIndent()
        val params = upperSynonyms.toTypedArray()
        return jdbcTemplate.query(sql, params) { rs, _ ->
            ConsultantSkillRow(
                rawSkillUpper = rs.getString("skill_upper").trim().uppercase(),
                consultantId = rs.getLong("consultant_id"),
                durationYears = rs.getInt("duration_years")
            )
        }
    }

    fun fetchFromCvProjectExperienceSkill(upperSynonyms: Set<String>): List<ConsultantSkillRow> {
        if (upperSynonyms.isEmpty()) return emptyList()
        val placeholders = upperSynonyms.joinToString(",") { "?" }
        val sql = """
            SELECT DISTINCT UPPER(TRIM(cps.skill)) AS skill_upper,
                            c.id AS consultant_id,
                            0 AS duration_years
            FROM cv_project_experience_skill cps
            JOIN cv_project_experience pe ON pe.id = cps.project_experience_id
            JOIN consultant_cv cc ON cc.id = pe.cv_id
            JOIN consultant c ON c.id = cc.consultant_id
            WHERE UPPER(cps.skill) IN ($placeholders)
        """.trimIndent()
        val params = upperSynonyms.toTypedArray()
        return jdbcTemplate.query(sql, params) { rs, _ ->
            ConsultantSkillRow(
                rawSkillUpper = rs.getString("skill_upper").trim().uppercase(),
                consultantId = rs.getLong("consultant_id"),
                durationYears = rs.getInt("duration_years")
            )
        }
    }

    fun countTotalConsultants(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consultant", Int::class.java) ?: 0
}
