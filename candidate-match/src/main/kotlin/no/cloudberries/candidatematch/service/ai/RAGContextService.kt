package no.cloudberries.candidatematch.service.ai

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import no.cloudberries.candidatematch.config.AIChatConfig
import no.cloudberries.candidatematch.infrastructure.integration.embedding.EmbeddingConfig
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.embedding.CvChunkEmbeddingRepository
import no.cloudberries.candidatematch.domain.embedding.EmbeddingProvider
import no.cloudberries.candidatematch.domain.consultant.Cv
import no.cloudberries.candidatematch.service.embedding.DomainCvTextFlattener
import org.springframework.stereotype.Service

@Service
class RAGContextService(
    private val consultantRepository: ConsultantRepository,
    private val embeddingProvider: EmbeddingProvider,
    private val chunkRepo: CvChunkEmbeddingRepository,
    private val aiChatConfig: AIChatConfig,
    private val embeddingConfig: EmbeddingConfig,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger { }

    data class RetrievedChunk(
        val index: Int,
        val text: String,
        val score: Double
    )

    fun ensureChunks(userId: String, cvId: String) {
        if (!embeddingProvider.isEnabled()) {
            logger.warn { "Embedding provider disabled, cannot create chunks" }
            return
        }
        if (chunkRepo.existsFor(userId, cvId, embeddingProvider.providerName, embeddingProvider.modelName)) {
            return
        }
        val consultant = consultantRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("Consultant not found: $userId")

// Flatten resume JSON to plain text using domain CV
        val domainCv = objectMapper.treeToValue(consultant.resumeData, Cv::class.java)
        val fullText = DomainCvTextFlattener.toText(domainCv)
        val chunks = chunkText(fullText, aiChatConfig.rag.chunkSize, aiChatConfig.rag.chunkOverlap)

        chunks.forEachIndexed { idx, chunk ->
            try {
                val vec = embeddingProvider.embed(chunk)
                if (vec.isNotEmpty()) {
                    chunkRepo.saveChunk(
                        userId = userId,
                        cvId = cvId,
                        chunkIndex = idx,
                        provider = embeddingProvider.providerName,
                        model = embeddingProvider.modelName,
                        text = chunk,
                        embedding = vec
                    )
                }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to embed chunk#$idx for $userId/$cvId" }
            }
        }
        logger.info { "Chunked and embedded CV for $userId/$cvId into ${chunks.size} chunks" }
    }

    fun retrieveTopK(userId: String, cvId: String, query: String, topK: Int): List<RetrievedChunk> {
        if (!embeddingProvider.isEnabled()) return emptyList()
        val queryVec = embeddingProvider.embed(query)
        if (queryVec.isEmpty()) return emptyList()
        val hits = chunkRepo.similaritySearch(
            queryEmbedding = queryVec,
            provider = embeddingProvider.providerName,
            model = embeddingProvider.modelName,
            userId = userId,
            cvId = cvId,
            topK = topK
        )
        return hits.map { RetrievedChunk(it.chunkIndex, it.text, 1.0 / (1.0 + it.distance)) }
    }

    private fun chunkText(text: String, chunkSize: Int, overlap: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val normalized = text.trim().replace(Regex("\n{3,}"), "\n\n")
        val chunks = mutableListOf<String>()
        var start = 0
        val size = normalized.length
        val step = (chunkSize - overlap).coerceAtLeast(1)
        while (start < size) {
            val end = (start + chunkSize).coerceAtMost(size)
            chunks.add(normalized.substring(start, end))
            if (end == size) break
            start += step
        }
        return chunks
    }
}
