package no.cloudberries.ai.rag

import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class DbIngestionService(
    private val jdbcTemplate: JdbcTemplate,
    private val vectorStore: VectorStore,
    private val tokenChunker: TokenChunker,
    @param:Value("\${rag.ingest.sql:}") private val sql: String,
    @param:Value("\${rag.ingest.id-column:candidate_id}") private val idColumn: String,
    @param:Value("\${rag.ingest.name-column:candidate_name}") private val nameColumn: String,
    @param:Value("\${rag.ingest.text-column:cv_text}") private val textColumn: String,
    @param:Value("\${rag.ingest.chunk.max-tokens:400}") private val maxTokens: Int,
    @param:Value("\${rag.ingest.chunk.overlap-tokens:50}") private val overlapTokens: Int
) {
    data class IngestReport(val rowsProcessed: Int, val chunksAdded: Int)

    fun ingestAll(): IngestReport {
        require(sql.isNotBlank()) { "rag.ingest.sql must be provided in application.yml" }

        var rows = 0
        var chunks = 0

        jdbcTemplate.query(sql) { rs ->
            val id = rs.getLong(idColumn)
            val name = rs.getString(nameColumn)
            val text = rs.getString(textColumn) ?: ""
            if (text.isNotBlank()) {
                val parts = tokenChunker.chunk(
                    text,
                    maxTokens,
                    overlapTokens
                )
                val docs = parts.mapIndexed { idx, chunk ->
                    val meta = linkedMapOf<String, Any>(
                        "type" to "cv",
                        "candidateId" to id.toString(),
                        "name" to (name ?: ""),
                        "chunkIndex" to idx
                    )
                    val docId = "cv:${id}:${idx}"
                    Document(
                        docId,
                        chunk,
                        meta
                    )
                }
                if (docs.isNotEmpty()) {
                    vectorStore.add(docs)
                    chunks += docs.size
                }
                rows++
            }
        }
        return IngestReport(
            rowsProcessed = rows,
            chunksAdded = chunks
        )
    }
}
