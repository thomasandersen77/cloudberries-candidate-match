package no.cloudberries.candidatematch.service.embedding

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.embedding.EmbeddingProvider
import no.cloudberries.candidatematch.infrastructure.integration.embedding.EmbeddingConfig
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseCvDto
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.embedding.CvEmbeddingRepository
import no.cloudberries.candidatematch.service.embedding.FlowcaseCvTextFlattener
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service

@Service
class CvEmbeddingService(
    private val consultantRepository: ConsultantRepository,
    private val embeddingProvider: EmbeddingProvider,
    private val repository: CvEmbeddingRepository,
    private val embeddingConfig: EmbeddingConfig,
    private val objectMapper: ObjectMapper,
) {
    private val logger = KotlinLogging.logger { }

    fun processJason(): Boolean {
        if (!embeddingProvider.isEnabled()) {
            logger.info { "Embedding is disabled; skipping Jason processing." }
            return false
        }
        val consultants = consultantRepository.findAll()
        val jason = consultants.firstOrNull {
            it.name.equals(
                "Jason",
                ignoreCase = true
            )
        } ?: run {
            logger.warn { "No consultant named 'Jason' found in database." }
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
        
        // Fetch consultant from database
        val consultant = consultantRepository.findByUserId(userId) ?: run {
            logger.warn { "No consultant found with userId=$userId" }
            return false
        }
        
        // Convert JsonNode resumeData to FlowcaseCvDto for text extraction
        val flowcaseCvDto = try {
            objectMapper.treeToValue(consultant.resumeData, FlowcaseCvDto::class.java)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse resume_data for userId=$userId: ${e.message}" }
            return false
        }
        
        val text = FlowcaseCvTextFlattener.toText(flowcaseCvDto)
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

    @Timed
    fun processMissingEmbeddings(batchSize: Int = 50): Int {
        if (!embeddingProvider.isEnabled()) {
            logger.info { "Embedding disabled; skipping scheduled processing." }
            return 0
        }
        
        // Fetch consultants from database (limit by batchSize)
        val consultants = consultantRepository.findAll().take(batchSize)
        var processed = 0
        
        consultants.forEach { consultant ->
            if (!repository.exists(
                    consultant.userId,
                    consultant.cvId,
                    embeddingProvider.providerName,
                    embeddingProvider.modelName
                )
            ) {
                try {
                    // Convert JsonNode resumeData to FlowcaseCvDto for text extraction
                    val flowcaseCvDto = objectMapper.treeToValue(consultant.resumeData, FlowcaseCvDto::class.java)
                    val text = FlowcaseCvTextFlattener.toText(flowcaseCvDto)
                    val vec = embeddingProvider.embed(text)
                    
                    if (vec.isNotEmpty()) {
                        repository.save(
                            consultant.userId,
                            consultant.cvId,
                            embeddingProvider.providerName,
                            embeddingProvider.modelName,
                            vec
                        )
                        processed++
                        logger.debug { "Generated embedding for consultant=${consultant.name}, userId=${consultant.userId}" }
                    } else {
                        logger.warn { "Empty embedding vector for consultant=${consultant.name}, userId=${consultant.userId}" }
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to process consultant=${consultant.name}, userId=${consultant.userId}: ${e.message}" }
                }
            }
        }
        logger.info { "Processed $processed embeddings in this run from ${consultants.size} consultants." }
        return processed
    }
}
