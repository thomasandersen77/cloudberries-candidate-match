package no.cloudberries.candidatematch.infrastructure.repositories.skills

import no.cloudberries.candidatematch.service.skills.ProjectSkillFetcher
import no.cloudberries.candidatematch.service.skills.SkillAggregateRow
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class JdbcProjectSkillFetcher(
    private val jdbcTemplate: JdbcTemplate
) : ProjectSkillFetcher {
    override fun fetch(normalizedFilters: Set<String>): List<SkillAggregateRow> {
        val sqlBase = """
            select upper(trim(ps.skill)) as skill_name,
                   c.user_id as user_id,
                   c.name as name,
                   c.cv_id as default_cv_id
            from cv_project_experience_skill ps
            join cv_project_experience pe on pe.id = ps.project_experience_id
            join consultant_cv cc on cc.id = pe.cv_id
            join consultant c on c.id = cc.consultant_id
        """.trimIndent()

        val rows = if (normalizedFilters.isEmpty()) {
            jdbcTemplate.query(sqlBase) { rs, _ ->
                SkillAggregateRow(
                    skillName = rs.getString("skill_name").trim(),
                    userId = rs.getString("user_id"),
                    name = rs.getString("name"),
                    defaultCvId = rs.getString("default_cv_id"),
                )
            }
        } else {
            val inClause = normalizedFilters.joinToString(",") { "?" }
            val sql = "$sqlBase where upper(trim(ps.skill)) in ($inClause)"
            jdbcTemplate.query(sql,  { rs, _ ->
                SkillAggregateRow(
                    skillName = rs.getString("skill_name").trim(),
                    userId = rs.getString("user_id"),
                    name = rs.getString("name"),
                    defaultCvId = rs.getString("default_cv_id"),
                )
            }, *normalizedFilters.toTypedArray())
        }
        return rows.distinctBy { it.skillName.uppercase() to it.userId }
    }
}
