package no.cloudberries.candidatematch.infrastructure.repositories.analytics

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class RoleStatsRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    data class ConsultantRoleRow(
        val roleNameUpper: String,
        val consultantId: Long,
    )

    fun fetchConsultantRoles(): List<ConsultantRoleRow> {
        val sql = """
            SELECT UPPER(TRIM(r.name)) AS role_name,
                   c.id AS consultant_id
            FROM cv_project_experience_role r
            JOIN cv_project_experience pe ON pe.id = r.project_experience_id
            JOIN consultant_cv cc ON cc.id = pe.cv_id
            JOIN consultant c ON c.id = cc.consultant_id
            WHERE r.name IS NOT NULL AND TRIM(r.name) <> ''
        """.trimIndent()
        return jdbcTemplate.query(sql) { rs, _ ->
            ConsultantRoleRow(
                roleNameUpper = rs.getString("role_name").trim().uppercase(),
                consultantId = rs.getLong("consultant_id")
            )
        }
    }

    fun countTotalConsultants(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consultant", Int::class.java) ?: 0
}