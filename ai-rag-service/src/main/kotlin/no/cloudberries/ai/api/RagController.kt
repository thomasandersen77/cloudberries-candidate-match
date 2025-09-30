package no.cloudberries.ai.api

import no.cloudberries.ai.rag.RagService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/rag")
class RagController(
    private val ragService: RagService,
    private val dbIngestionService: no.cloudberries.ai.rag.DbIngestionService
) {
    data class ChatRequest(
        val message: String,
        val topK: Int? = 4,
        val similarityThreshold: Double? = 0.7,
        val filter: String? = null
    )

    data class SourceDocument(
        val id: String?,
        val contentPreview: String,
        val metadata: Map<String, Any>
    )

    data class ChatResponse(
        val answer: String,
        val sources: List<SourceDocument>
    )

    data class IngestRequest(
        val candidateId: Long,
        val name: String? = null,
        val cvText: String
    )

    @PostMapping("/chat", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun chat(@RequestBody req: ChatRequest): ChatResponse {
        val result = ragService.chat(
            message = req.message,
            topK = req.topK ?: 4,
            threshold = req.similarityThreshold ?: 0.7,
            filter = req.filter
        )
        return ChatResponse(
            answer = result.answer,
            sources = result.sources.map { SourceDocument(it.id, it.contentPreview, it.metadata) }
        )
    }

    @PostMapping("/ingest", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun ingest(@RequestBody req: IngestRequest): Map<String, Any> {
        val count = ragService.ingestCv(
            candidateId = req.candidateId,
            name = req.name,
            cvText = req.cvText
        )
        return mapOf("ingested" to count)
    }

    @PostMapping("/ingest/db")
    fun ingestFromDb(): Map<String, Any> {
        val report = dbIngestionService.ingestAll()
        return mapOf(
            "rowsProcessed" to report.rowsProcessed,
            "chunksAdded" to report.chunksAdded
        )
    }
}
