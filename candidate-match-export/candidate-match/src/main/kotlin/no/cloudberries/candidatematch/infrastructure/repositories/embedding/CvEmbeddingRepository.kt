package no.cloudberries.candidatematch.infrastructure.repositories.embedding

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class CvEmbeddingRepository(
    private val jdbcTemplate: JdbcTemplate,
) {

    fun exists(userId: String, cvId: String, provider: String, model: String): Boolean {
        val sql = "SELECT 1 FROM cv_embedding WHERE user_id = ? AND cv_id = ? AND provider = ? AND model = ? LIMIT 1"
        return jdbcTemplate.query(
            sql,
            { rs, _ -> rs.getInt(1) },
            userId,
            cvId,
            provider,
            model
        ).isNotEmpty()
    }

    fun save(userId: String, cvId: String, provider: String, model: String, embedding: DoubleArray) {
        // Convert double[] to pgvector literal, e.g., '[0.1, -0.2, ...]'
        val vecLiteral = embedding.joinToString(
            prefix = "[",
            postfix = "]"
        ) { it.toString() }
        val sql = """
            INSERT INTO cv_embedding(user_id, cv_id, provider, model, embedding)
            VALUES (?, ?, ?, ?, ?::vector)
            ON CONFLICT (user_id, cv_id, provider, model) DO NOTHING
        """.trimIndent()
        jdbcTemplate.update(
            sql,
            userId,
            cvId,
            provider,
            model,
            vecLiteral
        )
    }
}