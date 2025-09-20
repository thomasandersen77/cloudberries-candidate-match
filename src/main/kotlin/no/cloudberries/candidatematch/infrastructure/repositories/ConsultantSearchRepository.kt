package no.cloudberries.candidatematch.infrastructure.repositories

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class ConsultantSearchRepository(
    @PersistenceContext
    private val entityManager: EntityManager
) {

    data class RelationalFilters(
        val name: String? = null,
        val skillsAll: Set<Skill> = emptySet(),
        val skillsAny: Set<Skill> = emptySet(),
        val minQualityScore: Int? = null,
        val onlyActiveCv: Boolean = false,
    )

    fun search(filters: RelationalFilters, pageable: Pageable): Page<ConsultantEntity> {
        // Base query from consultant with joins as needed
        val base = StringBuilder(
            "SELECT c.* FROM consultant c"
        )
        val where = mutableListOf<String>()
        val params = mutableMapOf<String, Any>()

        if (filters.name?.isNotBlank() == true) {
            where += "LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))"
            params["name"] = filters.name
        }
        if (filters.skillsAll.isNotEmpty() || filters.skillsAny.isNotEmpty()) {
            base.append(" LEFT JOIN consultant_skills cs ON cs.consultant_id = c.id")
        }
        if (filters.skillsAll.isNotEmpty()) {
            // Require that consultant has ALL skills in the set
            where += "NOT EXISTS (SELECT 1 FROM (SELECT UNNEST(CAST(:skillsAll AS text[])) s) req WHERE NOT EXISTS (SELECT 1 FROM consultant_skills cs2 WHERE cs2.consultant_id = c.id AND cs2.skill = s))"
            params["skillsAll"] = filters.skillsAll.map { it.name }
        }
        if (filters.skillsAny.isNotEmpty()) {
            // Has at least one of the skills
            where += "EXISTS (SELECT 1 FROM consultant_skills cs3 WHERE cs3.consultant_id = c.id AND cs3.skill = ANY(CAST(:skillsAny AS text[])))"
            params["skillsAny"] = filters.skillsAny.map { it.name }
        }
        if (filters.minQualityScore != null || filters.onlyActiveCv) {
            base.append(" LEFT JOIN consultant_cv cv ON cv.consultant_id = c.id")
        }
        if (filters.minQualityScore != null) {
            where += "cv.quality_score >= :minQuality"
            params["minQuality"] = filters.minQualityScore
        }
        if (filters.onlyActiveCv) {
            where += "cv.active = true"
        }

        if (where.isNotEmpty()) {
            base.append(" WHERE ")
            base.append(where.joinToString(" AND "))
        }
        base.append(" GROUP BY c.id")
        base.append(" ORDER BY c.name ASC")
        base.append(" LIMIT :limit OFFSET :offset")

        val query = entityManager.createNativeQuery(
            base.toString(),
            ConsultantEntity::class.java
        )
        params.forEach { (k, v) ->
            if (v is Collection<*>) {
                query.setParameter(
                    k,
                    v
                )
            } else {
                query.setParameter(
                    k,
                    v
                )
            }
        }
        query.setParameter(
            "limit",
            pageable.pageSize
        )
        query.setParameter(
            "offset",
            pageable.offset.toInt()
        )

        @Suppress("UNCHECKED_CAST")
        val content = query.resultList as List<ConsultantEntity>

        // Count query
        val countSql = StringBuilder("SELECT COUNT(DISTINCT c.id) FROM consultant c")
        if (filters.skillsAll.isNotEmpty() || filters.skillsAny.isNotEmpty()) {
            countSql.append(" LEFT JOIN consultant_skills cs ON cs.consultant_id = c.id")
        }
        if (filters.minQualityScore != null || filters.onlyActiveCv) {
            countSql.append(" LEFT JOIN consultant_cv cv ON cv.consultant_id = c.id")
        }
        if (where.isNotEmpty()) {
            countSql.append(" WHERE ").append(where.joinToString(" AND "))
        }
        val countQuery = entityManager.createNativeQuery(countSql.toString())
        params.forEach { (k, v) ->
            if (v is Collection<*>) countQuery.setParameter(
                k,
                v
            ) else countQuery.setParameter(
                k,
                v
            )
        }
        val total = (countQuery.singleResult as Number).toLong()

        return PageImpl(
            content,
            pageable,
            total
        )
    }
}
