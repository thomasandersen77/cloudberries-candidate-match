package no.cloudberries.candidatematch.controllers.embedding

import mu.KotlinLogging
import no.cloudberries.candidatematch.service.embedding.CvEmbeddingService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * ADMIN ONLY: Embedding management endpoints
 * 
 * TODO: Add Spring Security with @PreAuthorize("hasRole('ADMIN')")
 * These endpoints should only be accessible to administrators because:
 * - They can trigger expensive operations (Ollama embeddings for all CVs)
 * - They modify system-wide data (embeddings table)
 * - They can cause performance impact (batch processing)
 * 
 * For now, ensure these are NOT exposed publicly and document that
 * these are for admin/operations use only.
 */
@RestController
@RequestMapping("/embeddings")
class EmbeddingController(
    private val cvEmbeddingService: CvEmbeddingService,
) {
    private val logger = KotlinLogging.logger { }

    @PostMapping("/run/jason")
    @Timed
    fun runJason(): ResponseEntity<Map<String, Any>> {
        logger.info { "POST /api/embeddings/run/jason" }
        val result = cvEmbeddingService.processJason()
        return ResponseEntity.ok(mapOf("processedJason" to result))
    }

    @PostMapping("/run")
    @Timed
    fun runForUserCv(
        @RequestParam("userId") userId: String,
        @RequestParam("cvId") cvId: String,
    ): ResponseEntity<Map<String, Any>> {
        logger.info { "POST /api/embeddings/run userId=$userId cvId=$cvId" }
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
    @Timed
    fun runMissing(
        @RequestParam(
            name = "batchSize",
            defaultValue = "50"
        ) batchSize: Int
    ): ResponseEntity<Map<String, Any>> {
        logger.info { "POST /api/embeddings/run/missing batchSize=$batchSize" }
        val count = cvEmbeddingService.processMissingEmbeddings(batchSize)
        return ResponseEntity.ok(
            mapOf(
                "processedCount" to count,
                "batchSize" to batchSize
            )
        )
    }
}
