package no.cloudberries.candidatematch.infrastructure.repositories

import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.consultant.RelationalSearchCriteria
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * Repository for advanced consultant search operations
 */
@Repository
class ConsultantSearchRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val lexicon: no.cloudberries.candidatematch.config.SearchLexicon
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Finds consultants using relational criteria with skills matching
     */
    @Timed
    @Transactional(readOnly = true)
    fun findByRelationalCriteria(criteria: RelationalSearchCriteria, pageable: Pageable): Page<ConsultantFlatView> {
        logger.info { "Starting relational search with criteria: $criteria" }
        val whereConditions = mutableListOf<String>()
        val parameters = mutableListOf<Any>()

        // Base query with consultant and CV data
        val baseSelect = """
            SELECT DISTINCT c.id as id, c.user_id as user_id, c.name as name, c.cv_id as cv_id
            FROM consultant c
            JOIN consultant_cv cc ON c.id = cc.consultant_id
        """.trimIndent()

        var joinClause = ""
        var needsPeJoin = false

        // Handle name search
        if (!criteria.name.isNullOrBlank()) {
            whereConditions.add("LOWER(c.name) LIKE LOWER(?)")
            parameters.add("%${criteria.name}%")
        }

        // Handle quality score filter
        if (criteria.minQualityScore != null) {
            whereConditions.add("cc.quality_score IS NOT NULL AND cc.quality_score >= ?")
            parameters.add(criteria.minQualityScore)
        }

        // Handle active CV filter
        if (criteria.onlyActiveCv) {
            whereConditions.add("cc.active = true")
        }

        // Handle skills filtering - using cv_skill_in_category through consultant_cv
        var needsSkillJoins = false

        // Handle public sector and customer terms by joining project experiences
        val publicSectorTerms = lexicon.publicSectorTokens

        if (criteria.publicSector == true) {
            joinClause += """
                JOIN cv_project_experience pe ON cc.id = pe.cv_id
            """.trimIndent()
            needsPeJoin = true
            // Build OR conditions for public sector tokens
            val ors = mutableListOf<String>()
            publicSectorTerms.forEach { _ ->
                ors.add("LOWER(pe.customer) LIKE ?")
                ors.add("LOWER(pe.description) LIKE ?")
                ors.add("LOWER(pe.long_description) LIKE ?")
            }
            whereConditions.add("(" + ors.joinToString(" OR ") + ")")
            publicSectorTerms.forEach { t ->
                val pat = "%${t.lowercase()}%"
                parameters.add(pat); parameters.add(pat); parameters.add(pat)
            }
        }

        if (criteria.customersAny.isNotEmpty()) {
            if (!needsPeJoin) {
                joinClause += """
                    JOIN cv_project_experience pe ON cc.id = pe.cv_id
                """.trimIndent()
                needsPeJoin = true
            }
            val ors = mutableListOf<String>()
            criteria.customersAny.forEach { _ ->
                ors.add("LOWER(pe.customer) LIKE ?")
            }
            whereConditions.add("(" + ors.joinToString(" OR ") + ")")
            val expandedTerms = criteria.customersAny.flatMap { lexicon.expandCustomerTerm(it) }
            expandedTerms.forEach { t -> parameters.add("%${t.lowercase()}%") }
        }

        // Normalized industry filter via mapping table
        if (criteria.industriesAny.isNotEmpty()) {
            if (!needsPeJoin) {
                joinClause += """
                    JOIN cv_project_experience pe ON cc.id = pe.cv_id
                """.trimIndent()
                needsPeJoin = true
            }
            joinClause += """
                JOIN cv_project_experience_industry cpei ON pe.id = cpei.project_experience_id
                JOIN industry i ON cpei.industry_id = i.id
            """.trimIndent()
            whereConditions.add("LOWER(i.name) IN (" + criteria.industriesAny.joinToString(",") { "?" } + ")")
            criteria.industriesAny.forEach { parameters.add(it.lowercase()) }
        }

        // Skills that must ALL be present (AND condition)
        if (criteria.skillsAll.isNotEmpty()) {
            val matchingSkills = getMatchingSkillNames(criteria.skillsAll)
            if (matchingSkills.isNotEmpty()) {
                joinClause += """
                    JOIN cv_skill_category csc_all ON cc.id = csc_all.cv_id
                    JOIN cv_skill_in_category csic_all ON csc_all.id = csic_all.skill_category_id
                """.trimIndent()
                whereConditions.add("UPPER(csic_all.name) IN (${matchingSkills.joinToString(",") { "?" }})")
                parameters.addAll(matchingSkills.map { it.uppercase() })

                // Group by consultant and ensure all required skills are present
                val groupByClause = "GROUP BY c.id, c.user_id, c.name, c.cv_id, cc.quality_score, cc.active"
                val havingClause = "HAVING COUNT(DISTINCT UPPER(csic_all.name)) >= ?"
                parameters.add(matchingSkills.size - 1)

                return executePagedQuery(
                    baseSelect,
                    joinClause,
                    whereConditions,
                    parameters,
                    groupByClause,
                    havingClause,
                    pageable
                )
            }
        }

        // Skills where ANY can be present (OR condition)
        if (criteria.skillsAny.isNotEmpty()) {
            val matchingSkills = getMatchingSkillNames(criteria.skillsAny)
            if (matchingSkills.isNotEmpty()) {
                joinClause += """
                    JOIN cv_skill_category csc_any ON cc.id = csc_any.cv_id
                    JOIN cv_skill_in_category csic_any ON csc_any.id = csic_any.skill_category_id
                """.trimIndent()
                whereConditions.add("UPPER(csic_any.name) IN (${matchingSkills.joinToString(",") { "?" }})")
                parameters.addAll(matchingSkills.map { it.uppercase() })
                needsSkillJoins = true
            }
        }

        // If we have skills filtering, we need to use DISTINCT to avoid duplicates
        val finalGroupBy =
            if (needsSkillJoins) "GROUP BY c.id, c.user_id, c.name, c.cv_id, cc.quality_score, cc.active" else ""

        logger.info { "About to execute paged query with joinClause='$joinClause', whereConditions=$whereConditions, groupBy='$finalGroupBy'" }
        return executePagedQuery(
            baseSelect,
            joinClause,
            whereConditions,
            parameters,
            finalGroupBy,
            "",
            pageable
        )
    }

    /**
     * Finds consultants by semantic similarity using pgvector
     */
    @Timed
    @Transactional(readOnly = true)
    fun findBySemanticSimilarity(
        embedding: DoubleArray,
        provider: String,
        model: String,
        topK: Int,
        minQualityScore: Int? = null,
        onlyActiveCv: Boolean = false
    ): List<SemanticSearchResult> {

        val whereConditions = mutableListOf<String>()
        val parameters = mutableListOf<Any>()

        // Convert embedding to pgvector format
        val embeddingVector = embedding.joinToString(
            prefix = "[",
            postfix = "]"
        ) { it.toString() }

        val sql = """
            SELECT DISTINCT 
                c.id,
                c.user_id,
                c.name,
                c.cv_id,
                cc.quality_score,
                ce.embedding <-> ?::vector as distance
            FROM consultant c
            JOIN consultant_cv cc ON c.id = cc.consultant_id
            JOIN cv_embedding ce ON c.user_id = ce.user_id AND c.cv_id = ce.cv_id
            WHERE ce.provider = ? AND ce.model = ?
        """.trimIndent()

        parameters.add(embeddingVector)
        parameters.add(provider)
        parameters.add(model)

        // Add optional filters
        if (minQualityScore != null) {
            whereConditions.add("cc.quality_score IS NOT NULL AND cc.quality_score >= ?")
            parameters.add(minQualityScore)
        }

        if (onlyActiveCv) {
            whereConditions.add("cc.active = true")
        }

        val finalSql = sql +
                (if (whereConditions.isNotEmpty()) " AND " + whereConditions.joinToString(" AND ") else "") +
                " ORDER BY distance ASC LIMIT ?"

        parameters.add(topK)

        return jdbcTemplate.query(
            finalSql,
            { rs, _ ->
            SemanticSearchResult(
                id = rs.getLong("id"),
                userId = rs.getString("user_id"),
                name = rs.getString("name"),
                cvId = rs.getString("cv_id"),
                qualityScore = rs.getInt("quality_score"),
                distance = rs.getDouble("distance")
            )
        }, *parameters.toTypedArray()).toList()
    }

    /**
     * Hybrid re-ranking: restrict semantic similarity to a specific candidate list (userId, cvId pairs)
     */
    @Timed
    @Transactional(readOnly = true)
    fun reRankBySemanticSimilarity(
        embedding: DoubleArray,
        provider: String,
        model: String,
        allowedPairs: List<Pair<String, String>>, // (userId, cvId)
        topK: Int
    ): List<SemanticSearchResult> {
        if (allowedPairs.isEmpty()) return emptyList()
        val embeddingVector = embedding.joinToString(prefix = "[", postfix = "]") { it.toString() }
        // Build tuple list: (user_id, cv_id) IN ((?, ?), ...)
        val tuplePlaceholders = allowedPairs.joinToString(",") { "(?, ?)" }
        val sql = """
            SELECT DISTINCT c.id, c.user_id, c.name, c.cv_id, cc.quality_score, ce.embedding <-> ?::vector as distance
            FROM consultant c
            JOIN consultant_cv cc ON c.id = cc.consultant_id
            JOIN cv_embedding ce ON c.user_id = ce.user_id AND c.cv_id = ce.cv_id
            WHERE ce.provider = ? AND ce.model = ? AND (c.user_id, c.cv_id) IN ($tuplePlaceholders)
            ORDER BY distance ASC
            LIMIT ?
        """.trimIndent()
        val params = mutableListOf<Any>(embeddingVector, provider, model)
        allowedPairs.forEach { params.add(it.first); params.add(it.second) }
        params.add(topK)
        return jdbcTemplate.query(sql, { rs, _ ->
            SemanticSearchResult(
                id = rs.getLong("id"),
                userId = rs.getString("user_id"),
                name = rs.getString("name"),
                cvId = rs.getString("cv_id"),
                qualityScore = rs.getInt("quality_score"),
                distance = rs.getDouble("distance")
            )
        }, *params.toTypedArray()).toList()
    }

    /**
     * Helper method to get skill names that match (since skills are stored in cv_skill_in_category)
     */
    private fun getMatchingSkillNames(skillNames: List<String>): List<String> {
        if (skillNames.isEmpty()) return emptyList()

        val placeholders = skillNames.joinToString(",") { "?" }
        val sql = "SELECT DISTINCT name FROM cv_skill_in_category WHERE UPPER(name) IN ($placeholders)"
        val upperCaseNames = skillNames.map { it.uppercase() }

        return jdbcTemplate.query(
            sql, { rs, _ ->
            rs.getString("name")
        }, *upperCaseNames.toTypedArray()).toList()
    }

    /**
     * Data class implementation of ConsultantFlatView for easier instantiation
     */
    private data class ConsultantFlatViewImpl(
        private val id: Long,
        private val userId: String,
        private val name: String,
        private val cvId: String
    ) : ConsultantFlatView {
        override fun getId(): Long = id
        override fun getUserId(): String = userId
        override fun getName(): String = name
        override fun getCvId(): String = cvId
    }

    /**
     * Helper method to execute paged queries
     */
    private fun executePagedQuery(
        baseSelect: String,
        joinClause: String,
        whereConditions: List<String>,
        parameters: List<Any>,
        groupByClause: String,
        havingClause: String,
        pageable: Pageable
    ): Page<ConsultantFlatView> {
        logger.info { "executePagedQuery started" }

        val sqlClauses = buildSqlClauses(
            whereConditions,
            pageable
        )
        val totalElements = executeCountQuery(
            baseSelect,
            joinClause,
            sqlClauses,
            parameters
        )
        val consultants = executeDataQuery(
            baseSelect,
            joinClause,
            sqlClauses,
            parameters,
            pageable
        )

        logger.info { "Data query returned ${consultants.size} consultants" }
        return PageImpl(
            consultants,
            pageable,
            totalElements
        )
    }

    private data class SqlClauses(
        val whereClause: String,
        val orderByClause: String
    )

    private fun buildSqlClauses(whereConditions: List<String>, pageable: Pageable): SqlClauses {
        val whereClause = if (whereConditions.isNotEmpty()) {
            "WHERE " + whereConditions.joinToString(" AND ")
        } else ""

        val orderByClause = if (pageable.sort.isSorted) {
            "ORDER BY " + pageable.sort.map { "${it.property} ${it.direction}" }.joinToString(", ")
        } else {
            "ORDER BY c.name"
        }

        return SqlClauses(
            whereClause,
            orderByClause
        )
    }

    private fun executeCountQuery(
        baseSelect: String,
        joinClause: String,
        sqlClauses: SqlClauses,
        parameters: List<Any>
    ): Long {
        val countSql = """
        SELECT COUNT(*) FROM (
            $baseSelect 
            $joinClause 
            ${sqlClauses.whereClause}
        ) as counted
    """.trimIndent()

        logger.info { "About to execute count query: $countSql with parameters: $parameters" }
        val totalElements = jdbcTemplate.queryForObject(
            countSql,
            Long::class.java,
            *parameters.toTypedArray()
        ) ?: 0L
        logger.info { "Count query returned: $totalElements" }
        return totalElements
    }

    private fun executeDataQuery(
        baseSelect: String,
        joinClause: String,
        sqlClauses: SqlClauses,
        parameters: List<Any>,
        pageable: Pageable
    ): List<ConsultantFlatView> {
        val dataSql = """
        $baseSelect 
        $joinClause 
        ${sqlClauses.whereClause}
        ${sqlClauses.orderByClause}
        LIMIT ? OFFSET ?
    """.trimIndent()

        val dataParameters = parameters.toMutableList().apply {
            add(pageable.pageSize)
            add(pageable.offset)
        }

        logger.info { "About to execute data query: $dataSql with parameters: $dataParameters" }
        return jdbcTemplate.query(
            dataSql,
         { rs, _ ->
            ConsultantFlatViewImpl(
                id = rs.getLong("id"),
                userId = rs.getString("user_id"),
                name = rs.getString("name"),
                cvId = rs.getString("cv_id")
            )
        }, *dataParameters.toTypedArray()).toList()
    }
}

/**
 * Result object for semantic search
 */
data class SemanticSearchResult(
    val id: Long,
    val userId: String,
    val name: String,
    val cvId: String,
    val qualityScore: Int,
    val distance: Double
)