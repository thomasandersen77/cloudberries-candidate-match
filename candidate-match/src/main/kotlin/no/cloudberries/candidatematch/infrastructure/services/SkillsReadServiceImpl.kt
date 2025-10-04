package no.cloudberries.candidatematch.infrastructure.services

import no.cloudberries.candidatematch.controllers.skills.dto.SkillSummaryDto
import no.cloudberries.candidatematch.dto.consultants.ConsultantSummaryDto
import no.cloudberries.candidatematch.service.skills.SkillsReadService
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.DataAccessException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import kotlin.math.max
import kotlin.math.min

@Service
class SkillsReadServiceImpl(
    private val jdbcTemplate: JdbcTemplate,
) : SkillsReadService {

    @Cacheable(cacheNames = ["skillsSummaryCache"], key = "#root.methodName + '|' + (#q == null ? '' : #q) + '|' + #page + '|' + #size + '|' + (#sort == null ? '' : #sort)")
    override fun listSkillSummary(q: String?, page: Int, size: Int, sort: String?): Page<SkillSummaryDto> {
        val normalizedQ = q?.trim()?.takeIf { it.isNotEmpty() }
        val pageSafe = max(0, page)
        val sizeSafe = min(200, max(1, size))
        val (orderBy, orderDir) = parseSummarySort(sort)

        val where = if (normalizedQ != null) "WHERE name ILIKE '%' || ? || '%'" else ""
        val sqlData = """
            WITH base AS (
                SELECT cs.consultant_id AS consultant_id, s.name AS name
                FROM consultant_skill cs
                JOIN skill s ON s.id = cs.skill_id
                UNION
                SELECT c.id AS consultant_id, s.name AS name
                FROM cv_project_experience_skill_v2 ps
                JOIN skill s ON s.id = ps.skill_id
                JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
                JOIN consultant_cv cc ON cc.id = pe.cv_id
                JOIN consultant c ON c.id = cc.consultant_id
                UNION
                SELECT c.id AS consultant_id, ps.skill AS name
                FROM cv_project_experience_skill ps
                JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
                JOIN consultant_cv cc ON cc.id = pe.cv_id
                JOIN consultant c ON c.id = cc.consultant_id
                WHERE ps.skill IS NOT NULL AND TRIM(ps.skill) <> ''
                UNION
                SELECT c.id AS consultant_id, csic.name AS name
                FROM cv_skill_in_category csic
                JOIN cv_skill_category csc ON csic.skill_category_id = csc.id
                JOIN consultant_cv cc ON csc.cv_id = cc.id
                JOIN consultant c ON c.id = cc.consultant_id
                WHERE csic.name IS NOT NULL AND TRIM(csic.name) <> ''
            )
            SELECT UPPER(TRIM(name)) AS name,
                   COUNT(DISTINCT consultant_id) AS consultant_count
            FROM base
            $where
            GROUP BY name
            ORDER BY $orderBy $orderDir, name ASC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val paramsData = mutableListOf<Any>()
        if (normalizedQ != null) paramsData.add(normalizedQ)
        paramsData.add(sizeSafe)
        paramsData.add(pageSafe * sizeSafe)

        val rows = jdbcTemplate.query(sqlData, paramsData.toTypedArray()) { rs, _ ->
            SkillSummaryDto(
                name = rs.getString("name").trim(),
                consultantCount = rs.getInt("consultant_count")
            )
        }

        val sqlCount = """
            WITH base AS (
                SELECT cs.consultant_id AS consultant_id, s.name AS name
                FROM consultant_skill cs
                JOIN skill s ON s.id = cs.skill_id
                UNION
                SELECT c.id AS consultant_id, s.name AS name
                FROM cv_project_experience_skill_v2 ps
                JOIN skill s ON s.id = ps.skill_id
                JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
                JOIN consultant_cv cc ON cc.id = pe.cv_id
                JOIN consultant c ON c.id = cc.consultant_id
                UNION
                SELECT c.id AS consultant_id, ps.skill AS name
                FROM cv_project_experience_skill ps
                JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
                JOIN consultant_cv cc ON cc.id = pe.cv_id
                JOIN consultant c ON c.id = cc.consultant_id
                WHERE ps.skill IS NOT NULL AND TRIM(ps.skill) <> ''
                UNION
                SELECT c.id AS consultant_id, csic.name AS name
                FROM cv_skill_in_category csic
                JOIN cv_skill_category csc ON csic.skill_category_id = csc.id
                JOIN consultant_cv cc ON csc.cv_id = cc.id
                JOIN consultant c ON c.id = cc.consultant_id
                WHERE csic.name IS NOT NULL AND TRIM(csic.name) <> ''
            )
            SELECT COUNT(*)
            FROM (
              SELECT name
              FROM base
              $where
              GROUP BY name
            ) t
        """.trimIndent()
        val paramsCount = if (normalizedQ != null) arrayOf(normalizedQ) else emptyArray()
        val total = jdbcTemplate.queryForObject(sqlCount, paramsCount, Long::class.java) ?: 0L

        return PageImpl(rows, org.springframework.data.domain.PageRequest.of(pageSafe, sizeSafe), total)
    }

    @Cacheable(cacheNames = ["consultantsBySkillCache"], key = "#root.methodName + '|' + (#skill == null ? '' : #skill.toUpperCase()) + '|' + #page + '|' + #size + '|' + (#sort == null ? '' : #sort)")
    override fun listConsultantsBySkill(skill: String, page: Int, size: Int, sort: String?): Page<ConsultantSummaryDto> {
        val skillNorm = skill.trim()
        val pageSafe = max(0, page)
        val sizeSafe = min(200, max(1, size))
        val orderBy = parseConsultantSort(sort)

        val sqlData = """
            WITH base AS (
                SELECT c.user_id AS user_id,
                       c.name AS name,
                       c.cv_id AS default_cv_id,
                       s.name AS skill_name
                FROM consultant c
                JOIN consultant_skill cs ON cs.consultant_id = c.id
                JOIN skill s ON s.id = cs.skill_id
                UNION
                SELECT c.user_id AS user_id,
                       c.name AS name,
                       c.cv_id AS default_cv_id,
                       s.name AS skill_name
                FROM cv_project_experience_skill_v2 ps
                JOIN skill s ON s.id = ps.skill_id
                JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
                JOIN consultant_cv cc ON cc.id = pe.cv_id
                JOIN consultant c ON c.id = cc.consultant_id
                UNION
                SELECT c.user_id AS user_id,
                       c.name AS name,
                       c.cv_id AS default_cv_id,
                       ps.skill AS skill_name
                FROM cv_project_experience_skill ps
                JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
                JOIN consultant_cv cc ON cc.id = pe.cv_id
                JOIN consultant c ON c.id = cc.consultant_id
                WHERE ps.skill IS NOT NULL AND TRIM(ps.skill) <> ''
                UNION
                SELECT c.user_id AS user_id,
                       c.name AS name,
                       c.cv_id AS default_cv_id,
                       csic.name AS skill_name
                FROM cv_skill_in_category csic
                JOIN cv_skill_category csc ON csic.skill_category_id = csc.id
                JOIN consultant_cv cc ON csc.cv_id = cc.id
                JOIN consultant c ON c.id = cc.consultant_id
                WHERE csic.name IS NOT NULL AND TRIM(csic.name) <> ''
            )
            SELECT DISTINCT user_id, name, default_cv_id
            FROM base
            WHERE UPPER(TRIM(skill_name)) = UPPER(TRIM(?))
            ORDER BY $orderBy
            LIMIT ? OFFSET ?
        """.trimIndent()

        val rows = jdbcTemplate.query(sqlData, arrayOf(skillNorm, sizeSafe, pageSafe * sizeSafe)) { rs, _ ->
            ConsultantSummaryDto(
                userId = rs.getString("user_id"),
                name = rs.getString("name"),
                email = "", // not available here
                bornYear = 0, // not available here
                defaultCvId = rs.getString("default_cv_id")
            )
        }

        val sqlCount = """
            WITH base AS (
                SELECT c.id AS consultant_id,
                       s.name AS skill_name
                FROM consultant c
                JOIN consultant_skill cs ON cs.consultant_id = c.id
                JOIN skill s ON s.id = cs.skill_id
                UNION
                SELECT c.id AS consultant_id,
                       s.name AS skill_name
                FROM cv_project_experience_skill_v2 ps
                JOIN skill s ON s.id = ps.skill_id
                JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
                JOIN consultant_cv cc ON cc.id = pe.cv_id
                JOIN consultant c ON c.id = cc.consultant_id
                UNION
                SELECT c.id AS consultant_id,
                       ps.skill AS skill_name
                FROM cv_project_experience_skill ps
                JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
                JOIN consultant_cv cc ON cc.id = pe.cv_id
                JOIN consultant c ON c.id = cc.consultant_id
                WHERE ps.skill IS NOT NULL AND TRIM(ps.skill) <> ''
                UNION
                SELECT c.id AS consultant_id,
                       csic.name AS skill_name
                FROM cv_skill_in_category csic
                JOIN cv_skill_category csc ON csic.skill_category_id = csc.id
                JOIN consultant_cv cc ON csc.cv_id = cc.id
                JOIN consultant c ON c.id = cc.consultant_id
                WHERE csic.name IS NOT NULL AND TRIM(csic.name) <> ''
            )
            SELECT COUNT(DISTINCT consultant_id)
            FROM base
            WHERE UPPER(TRIM(skill_name)) = UPPER(TRIM(?))
        """.trimIndent()
        val total = jdbcTemplate.queryForObject(sqlCount, arrayOf(skillNorm), Long::class.java) ?: 0L

        return PageImpl(rows, org.springframework.data.domain.PageRequest.of(pageSafe, sizeSafe), total)
    }

    @Cacheable(cacheNames = ["skillNamesCache"], key = "#root.methodName + '|' + (#prefix == null ? '' : #prefix) + '|' + #limit")
    override fun listSkillNames(prefix: String?, limit: Int): List<String> {
        val limitSafe = min(200, max(1, limit))
        val normalized = prefix?.trim()?.takeIf { it.isNotEmpty() }

        val base = """
            WITH base AS (
              SELECT cs.consultant_id AS consultant_id, UPPER(TRIM(s.name)) AS name
              FROM consultant_skill cs
              JOIN skill s ON s.id = cs.skill_id
              UNION
              SELECT c.id AS consultant_id, UPPER(TRIM(s2.name)) AS name
              FROM cv_project_experience_skill_v2 ps
              JOIN skill s2 ON s2.id = ps.skill_id
              JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
              JOIN consultant_cv cc ON cc.id = pe.cv_id
              JOIN consultant c ON c.id = cc.consultant_id
              UNION
              SELECT c.id AS consultant_id, UPPER(TRIM(ps.skill)) AS name
              FROM cv_project_experience_skill ps
              JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
              JOIN consultant_cv cc ON cc.id = pe.cv_id
              JOIN consultant c ON c.id = cc.consultant_id
              WHERE ps.skill IS NOT NULL AND TRIM(ps.skill) <> ''
              UNION
              SELECT c.id AS consultant_id, UPPER(TRIM(csic.name)) AS name
              FROM cv_skill_in_category csic
              JOIN cv_skill_category csc ON csic.skill_category_id = csc.id
              JOIN consultant_cv cc ON csc.cv_id = cc.id
              JOIN consultant c ON c.id = cc.consultant_id
              WHERE csic.name IS NOT NULL AND TRIM(csic.name) <> ''
            )
        """.trimIndent()

        return if (normalized == null) {
            val sql = """
                $base
                SELECT name
                FROM base
                GROUP BY name
                ORDER BY COUNT(DISTINCT consultant_id) DESC, name ASC
                LIMIT ?
            """.trimIndent()
            jdbcTemplate.query(sql, arrayOf(limitSafe)) { rs, _ -> rs.getString("name").trim() }
        } else {
            val sql = """
                $base
                SELECT name
                FROM base
                WHERE name ILIKE ? || '%'
                GROUP BY name
                ORDER BY COUNT(DISTINCT consultant_id) DESC, name ASC
                LIMIT ?
            """.trimIndent()
            jdbcTemplate.query(sql, arrayOf(normalized, limitSafe)) { rs, _ -> rs.getString("name").trim() }
        }
    }

    @Cacheable(cacheNames = ["topConsultantsBySkillCache"], key = "#root.methodName + '|' + (#skill == null ? '' : #skill.toUpperCase()) + '|' + #limit")
    override fun listTopConsultantsBySkill(skill: String, limit: Int): List<ConsultantSummaryDto> {
        val skillNorm = skill.trim()
        val limitSafe = min(50, max(1, limit))
        val sql = """
            WITH base AS (
                SELECT c.user_id AS user_id,
                       c.name AS name,
                       c.cv_id AS default_cv_id,
                       s.name AS skill_name
                FROM consultant c
                JOIN consultant_skill cs ON cs.consultant_id = c.id
                JOIN skill s ON s.id = cs.skill_id
                UNION
                SELECT c.user_id AS user_id,
                       c.name AS name,
                       c.cv_id AS default_cv_id,
                       s.name AS skill_name
                FROM cv_project_experience_skill_v2 ps
                JOIN skill s ON s.id = ps.skill_id
                JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
                JOIN consultant_cv cc ON cc.id = pe.cv_id
                JOIN consultant c ON c.id = cc.consultant_id
                UNION
                SELECT c.user_id AS user_id,
                       c.name AS name,
                       c.cv_id AS default_cv_id,
                       ps.skill AS skill_name
                FROM cv_project_experience_skill ps
                JOIN cv_project_experience pe ON pe.id = ps.project_experience_id
                JOIN consultant_cv cc ON cc.id = pe.cv_id
                JOIN consultant c ON c.id = cc.consultant_id
                WHERE ps.skill IS NOT NULL AND TRIM(ps.skill) <> ''
                UNION
                SELECT c.user_id AS user_id,
                       c.name AS name,
                       c.cv_id AS default_cv_id,
                       csic.name AS skill_name
                FROM cv_skill_in_category csic
                JOIN cv_skill_category csc ON csic.skill_category_id = csc.id
                JOIN consultant_cv cc ON csc.cv_id = cc.id
                JOIN consultant c ON c.id = cc.consultant_id
                WHERE csic.name IS NOT NULL AND TRIM(csic.name) <> ''
            ), users AS (
                SELECT DISTINCT user_id, name, default_cv_id
                FROM base
                WHERE UPPER(TRIM(skill_name)) = UPPER(TRIM(?))
            )
            SELECT u.user_id, u.name, u.default_cv_id
            FROM users u
            LEFT JOIN cv_score cs ON cs.candidate_user_id = u.user_id
            ORDER BY COALESCE(cs.score_percent, -1) DESC, u.name ASC
            LIMIT ?
        """.trimIndent()
        return jdbcTemplate.query(sql, arrayOf(skillNorm, limitSafe)) { rs, _ ->
            ConsultantSummaryDto(
                userId = rs.getString("user_id"),
                name = rs.getString("name"),
                email = "",
                bornYear = 0,
                defaultCvId = rs.getString("default_cv_id")
            )
        }
    }

    private fun parseSummarySort(raw: String?): Pair<String, String> {
        // Allowed: consultantCount desc|asc, name asc|desc
        if (raw == null || raw.isBlank()) return "consultant_count" to "DESC"
        val parts = raw.split(',').map { it.trim() }
        if (parts.isEmpty()) return "consultant_count" to "DESC"
        val field = parts[0].lowercase()
        val dir = parts.getOrNull(1)?.uppercase() ?: "ASC"
        val safeDir = if (dir == "DESC") "DESC" else "ASC"
        return when (field) {
            "consultantcount" -> "consultant_count" to safeDir
            "name" -> "name" to safeDir
            else -> "consultant_count" to "DESC"
        }
    }

    private fun parseConsultantSort(raw: String?): String {
        // Only allow name asc/desc
        if (raw == null || raw.isBlank()) return "name ASC"
        val parts = raw.split(',').map { it.trim() }
        val field = parts.getOrNull(0)?.lowercase()
        val dir = parts.getOrNull(1)?.uppercase() ?: "ASC"
        val safeDir = if (dir == "DESC") "DESC" else "ASC"
        return if (field == "name") "name $safeDir" else "name ASC"
    }
}
