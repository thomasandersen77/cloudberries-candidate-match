package no.cloudberries.candidatematch.infrastructure.repositories.embedding

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class CvChunkEmbeddingRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    fun existsFor(userId: String, cvId: String, provider: String, model: String): Boolean {
        val sql = "SELECT 1 FROM cv_chunk_embedding WHERE user_id = ? AND cv_id = ? AND provider = ? AND model = ? LIMIT 1"
        return jdbcTemplate.query(sql, { rs, _ -> rs.getInt(1) }, userId, cvId, provider, model).isNotEmpty()
    }

    fun saveChunk(
        userId: String,
        cvId: String,
        chunkIndex: Int,
        provider: String,
        model: String,
        text: String,
        embedding: DoubleArray
    ) {
        val vecLiteral = embedding.joinToString(prefix = "[", postfix = "]") { it.toString() }
        val sql = """
            INSERT INTO cv_chunk_embedding(user_id, cv_id, chunk_index, provider, model, text, embedding)
            VALUES (?, ?, ?, ?, ?, ?, ?::vector)
            ON CONFLICT (user_id, cv_id, chunk_index, provider, model) DO NOTHING
        """.trimIndent()
        jdbcTemplate.update(sql, userId, cvId, chunkIndex, provider, model, text, vecLiteral)
    }

    data class ChunkHit(
        val chunkIndex: Int,
        val text: String,
        val distance: Double
    )

    fun similaritySearch(
        queryEmbedding: DoubleArray,
        provider: String,
        model: String,
        userId: String,
        cvId: String,
        topK: Int
    ): List<ChunkHit> {
        val vecLiteral = queryEmbedding.joinToString(prefix = "[", postfix = "]") { it.toString() }
        val sql = """
            SELECT chunk_index, text, embedding <-> ?::vector as distance
            FROM cv_chunk_embedding
            WHERE provider = ? AND model = ? AND user_id = ? AND cv_id = ?
            ORDER BY distance ASC
            LIMIT ?
        """.trimIndent()
        return jdbcTemplate.query(sql, { rs, _ ->
            ChunkHit(
                chunkIndex = rs.getInt("chunk_index"),
                text = rs.getString("text"),
                distance = rs.getDouble("distance")
            )
        }, vecLiteral, provider, model, userId, cvId, topK).toList()
    }
}
