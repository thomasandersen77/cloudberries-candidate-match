package no.cloudberries.candidatematch.controllers.embedding

import no.cloudberries.candidatematch.service.embedding.CvEmbeddingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/embeddings")
class EmbeddingController(
    private val cvEmbeddingService: CvEmbeddingService,
) {
    @PostMapping("/run/jason")
    fun runJason(): ResponseEntity<Map<String, Any>> {
        val result = cvEmbeddingService.processJason()
        return ResponseEntity.ok(mapOf("processedJason" to result))
    }

    @PostMapping("/run")
    fun runForUserCv(
        @org.springframework.web.bind.annotation.RequestParam("userId") userId: String,
        @org.springframework.web.bind.annotation.RequestParam("cvId") cvId: String,
    ): ResponseEntity<Map<String, Any>> {
        val processed = cvEmbeddingService.processUserCv(
            userId,
            cvId
        )
        return ResponseEntity.ok(
            mapOf(
                "userId" to userId,
                "cvId" to cvId,
                "processed" to processed
            )
        )
    }

    @PostMapping("/run/missing")
    fun runMissing(
        @org.springframework.web.bind.annotation.RequestParam(
            name = "batchSize",
            defaultValue = "50"
        ) batchSize: Int
    ): ResponseEntity<Map<String, Any>> {
        val count = cvEmbeddingService.processMissingEmbeddings(batchSize)
        return ResponseEntity.ok(
            mapOf(
                "processedCount" to count,
                "batchSize" to batchSize
            )
        )
    }
}
