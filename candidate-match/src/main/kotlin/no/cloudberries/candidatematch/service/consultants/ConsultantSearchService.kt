package no.cloudberries.candidatematch.service.consultants

import mu.KotlinLogging
import no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto
import no.cloudberries.candidatematch.domain.consultant.RelationalSearchCriteria
import no.cloudberries.candidatematch.domain.consultant.SemanticSearchCriteria
import no.cloudberries.candidatematch.domain.embedding.EmbeddingProvider
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantSearchRepository
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for advanced consultant search operations combining relational and semantic search
 */
@Service
@Transactional(readOnly = true)
class ConsultantSearchService(
    private val consultantSearchRepository: ConsultantSearchRepository,
    private val embeddingProvider: EmbeddingProvider,
    private val cvDataAggregationService: CvDataAggregationService
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Performs relational (structured) search for consultants based on criteria
     */
    @Transactional
    @Timed
    fun searchRelational(criteria: RelationalSearchCriteria, pageable: Pageable): Page<ConsultantWithCvDto> {
        logger.info { "Performing relational search with criteria: name=${criteria.name}, skillsAll=${criteria.skillsAll}, skillsAny=${criteria.skillsAny}" }

        // Validate criteria
        val validationErrors = criteria.validate()
        if (validationErrors.isNotEmpty()) {
            throw IllegalArgumentException("Invalid search criteria: ${validationErrors.joinToString(", ")}")
        }

        // Execute search
        val consultantFlats = consultantSearchRepository.findByRelationalCriteria(
            criteria,
            pageable
        )

        if (consultantFlats.isEmpty) {
            logger.info { "No consultants found matching relational criteria" }
            return PageImpl(
                emptyList(),
                pageable,
                0
            )
        }

        // Build full consultant DTOs with CV data
        val consultantIds = consultantFlats.content.map { it.getId() }
        val cvDataByConsultant = cvDataAggregationService.aggregateCvData(
            consultantIds,
            criteria.onlyActiveCv
        )

        val consultantDtos = consultantFlats.content.map { consultant ->
            ConsultantWithCvDto(
                id = consultant.getId(),
                userId = consultant.getUserId(),
                name = consultant.getName(),
                cvId = consultant.getCvId(),
                skills = emptyList(), // Skills are included in CV data
                cvs = cvDataByConsultant[consultant.getId()] ?: emptyList()
            )
        }

        logger.info { "Relational search returned ${consultantDtos.size} consultants" }
        return PageImpl(
            consultantDtos,
            pageable,
            consultantFlats.totalElements
        )
    }

    /**
     * Performs semantic search for consultants using embeddings and natural language
     */
    @Timed
    @Transactional
    fun searchSemantic(criteria: SemanticSearchCriteria, pageable: Pageable): Page<ConsultantWithCvDto> {
        logger.info { "Performing semantic search with text: '${criteria.text}', provider: ${criteria.provider}, model: ${criteria.model}" }

        // Validate criteria
        val validationErrors = criteria.validate()
        if (validationErrors.isNotEmpty()) {
            throw IllegalArgumentException("Invalid search criteria: ${validationErrors.joinToString(", ")}")
        }

        // Check if embedding provider is enabled
        if (!embeddingProvider.isEnabled()) {
            logger.warn { "Embedding provider is disabled, cannot perform semantic search" }
            throw IllegalStateException("Semantic search is not available - embedding provider is disabled")
        }

        // Validate provider and model match
        if (criteria.provider != embeddingProvider.providerName || criteria.model != embeddingProvider.modelName) {
            logger.warn { "Provider/model mismatch: requested ${criteria.provider}/${criteria.model}, available ${embeddingProvider.providerName}/${embeddingProvider.modelName}" }
            throw IllegalArgumentException("Provider/model mismatch. Available: ${embeddingProvider.providerName}/${embeddingProvider.modelName}")
        }

        // Generate embedding for search text
        val searchEmbedding = try {
            embeddingProvider.embed(criteria.text)
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate embedding for search text: '${criteria.text}'" }
            throw RuntimeException(
                "Failed to generate search embedding",
                e
            )
        }

        if (searchEmbedding.isEmpty()) {
            logger.warn { "Empty embedding generated for search text: '${criteria.text}'" }
            throw RuntimeException("Empty embedding generated for search text")
        }

        // Execute semantic search
        val semanticResults = consultantSearchRepository.findBySemanticSimilarity(
            embedding = searchEmbedding,
            provider = criteria.provider,
            model = criteria.model,
            topK = criteria.topK,
            minQualityScore = criteria.minQualityScore,
            onlyActiveCv = criteria.onlyActiveCv
        )

        if (semanticResults.isEmpty()) {
            logger.info { "No consultants found matching semantic criteria" }
            return PageImpl(
                emptyList(),
                pageable,
                0
            )
        }

        // Apply pagination to semantic results
        val startIndex = pageable.offset.toInt()
        val endIndex = minOf(
            startIndex + pageable.pageSize,
            semanticResults.size
        )
        val pagedResults = if (startIndex < semanticResults.size) {
            semanticResults.subList(
                startIndex,
                endIndex
            )
        } else {
            emptyList()
        }

        // Build full consultant DTOs with CV data
        val consultantIds = pagedResults.map { it.id }
        val cvDataByConsultant = cvDataAggregationService.aggregateCvData(
            consultantIds,
            criteria.onlyActiveCv
        )

        val consultantDtos = pagedResults.map { result ->
            ConsultantWithCvDto(
                id = result.id,
                userId = result.userId,
                name = result.name,
                cvId = result.cvId,
                skills = emptyList(), // Skills are included in CV data
                cvs = cvDataByConsultant[result.id] ?: emptyList()
            )
        }

        logger.info { "Semantic search returned ${consultantDtos.size} consultants out of ${semanticResults.size} total matches" }
        return PageImpl(
            consultantDtos,
            pageable,
            semanticResults.size.toLong()
        )
    }

    /**
     * Gets the available embedding provider information for semantic search
     */
    fun getEmbeddingProviderInfo(): EmbeddingProviderInfo {
        return EmbeddingProviderInfo(
            enabled = embeddingProvider.isEnabled(),
            provider = embeddingProvider.providerName,
            model = embeddingProvider.modelName,
            dimension = embeddingProvider.dimension
        )
    }
}

/**
 * Information about the available embedding provider
 */
data class EmbeddingProviderInfo(
    val enabled: Boolean,
    val provider: String,
    val model: String,
    val dimension: Int
)