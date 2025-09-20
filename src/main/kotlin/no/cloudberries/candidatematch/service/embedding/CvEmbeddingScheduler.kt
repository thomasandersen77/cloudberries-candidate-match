package no.cloudberries.candidatematch.service.embedding

import mu.KotlinLogging
import no.cloudberries.candidatematch.infrastructure.integration.embedding.EmbeddingConfig
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CvEmbeddingScheduler(
    private val embeddingConfig: EmbeddingConfig,
    private val cvEmbeddingService: CvEmbeddingService,
) {
    private val logger = KotlinLogging.logger { }

    // Hver time
    @Scheduled(cron = "0 0 23 * * ?")
    fun scheduleEmbeddingJob() {
        if (!embeddingConfig.enabled) {
            logger.debug { "Embedding disabled; scheduler skipping run." }
            return
        }
        logger.info { "Scheduler: starting embedding job..." }
        cvEmbeddingService.processMissingEmbeddings()
    }
}