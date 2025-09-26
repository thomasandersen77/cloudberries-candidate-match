package no.cloudberries.candidatematch.service.embedding

import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.consultant.toFlatText
import no.cloudberries.candidatematch.domain.embedding.EmbeddingProvider
import no.cloudberries.candidatematch.infrastructure.integration.embedding.EmbeddingConfig
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.toDomain
import no.cloudberries.candidatematch.infrastructure.repositories.embedding.CvEmbeddingRepository
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service

@Service
class CvEmbeddingService(
    private val flowcaseHttpClient: FlowcaseHttpClient,
    private val embeddingProvider: EmbeddingProvider,
    private val repository: CvEmbeddingRepository,
    private val embeddingConfig: EmbeddingConfig,
) {
    private val logger = KotlinLogging.logger { }

    fun processJason(): Boolean {
        if (!embeddingProvider.isEnabled()) {
            logger.info { "Embedding is disabled; skipping Jason processing." }
            return false
        }
        val users = flowcaseHttpClient.fetchAllUsers().flowcaseUserDTOs
        val jason = users.firstOrNull {
            it.name.equals(
                "Jason",
                ignoreCase = true
            )
        } ?: run {
            logger.warn { "No user named 'Jason' found from Flowcase." }
            return false
        }
        return processUserCv(
            jason.userId,
            jason.cvId
        )
    }

    @Timed
    fun processUserCv(userId: String, cvId: String): Boolean {
        if (!embeddingProvider.isEnabled()) {
            logger.info { "Embedding is disabled; skipping processing for userId=$userId, cvId=$cvId." }
            return false
        }
        if (repository.exists(
                userId,
                cvId,
                embeddingProvider.providerName,
                embeddingProvider.modelName
            )
        ) {
            logger.info { "Embedding already exists for userId=$userId, cvId=$cvId." }
            return false
        }
        val cvDomain = flowcaseHttpClient.fetchCompleteCv(
            userId,
            cvId
        ).toDomain()
        val text = cvDomain.toFlatText()
        val vec = embeddingProvider.embed(text)
        if (vec.isEmpty()) {
            logger.warn { "Embedding provider returned empty vector for userId=$userId, cvId=$cvId. Skipping save." }
            return false
        }
        repository.save(
            userId,
            cvId,
            embeddingProvider.providerName,
            embeddingProvider.modelName,
            vec
        )
        logger.info { "Saved embedding for userId=$userId, cvId=$cvId." }
        return true
    }

    fun processMissingEmbeddings(batchSize: Int = 50): Int {
        if (!embeddingProvider.isEnabled()) {
            logger.debug { "Embedding disabled; skipping scheduled processing." }
            return 0
        }
        val users = flowcaseHttpClient.fetchAllUsers().flowcaseUserDTOs.take(batchSize)
        var processed = 0
        users.forEach { u ->
            if (!repository.exists(
                    u.userId,
                    u.cvId,
                    embeddingProvider.providerName,
                    embeddingProvider.modelName
                )
            ) {
                val cvDomain = flowcaseHttpClient.fetchCompleteCv(
                    u.userId,
                    u.cvId
                ).toDomain()
                val text = cvDomain.toFlatText()
                val vec = embeddingProvider.embed(text)
                if (vec.isNotEmpty()) {
                    repository.save(
                        u.userId,
                        u.cvId,
                        embeddingProvider.providerName,
                        embeddingProvider.modelName,
                        vec
                    )
                    processed++
                }
            }
        }
        logger.info { "Processed $processed embeddings in this run." }
        return processed
    }
}
