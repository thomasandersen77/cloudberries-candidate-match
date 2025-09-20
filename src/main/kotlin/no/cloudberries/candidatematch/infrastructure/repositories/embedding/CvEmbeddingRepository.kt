package no.cloudberries.candidatematch.infrastructure.repositories.embedding

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class CvEmbeddingRepository(
    private val jdbcTemplate: JdbcTemplate,
) {

    data class VectorSearchHit(
        val userId: String,
        val cvId: String,
        val distance: Double,
        val similarity: Double,
    )

    fun exists(userId: String, cvId: String, provider: String, model: String): Boolean {
        val sql = """
                SELECT 1 FROM cv_embedding
                WHERE user_id = ? AND cv_id = ? 
                AND provider = ? AND model = ? LIMIT 1"""
            .trimMargin()
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

    /**
     * Vector similarity search using pgvector cosine distance operator (<=>).
     * Returns top K most similar CV embeddings for a given provider/model.
     * Note: Similarity is computed as 1 - distance (assumes normalized vectors).
     */
    fun searchSimilar(
        queryVector: DoubleArray,
        provider: String,
        model: String,
        topK: Int = 10,
    ): List<VectorSearchHit> {
        val vecLiteral = queryVector.joinToString(
            prefix = "[",
            postfix = "]"
        ) { it.toString() }
        val sql = """
            SELECT user_id, cv_id,
                   (embedding <=> ?::vector) AS distance,
                   1 - (embedding <=> ?::vector) AS similarity
            FROM cv_embedding
            WHERE provider = ? AND model = ?
            ORDER BY embedding <=> ?::vector ASC
            LIMIT ?
        """.trimIndent()
        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                VectorSearchHit(
                    userId = rs.getString("user_id"),
                    cvId = rs.getString("cv_id"),
                    distance = rs.getDouble("distance"),
                    similarity = rs.getDouble("similarity"),
                )
            },
            vecLiteral,
            vecLiteral,
            provider,
            model,
            vecLiteral,
            topK
        )
    }
}
