package no.cloudberries.candidatematch.service.consultants

import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantSearchRepository
import no.cloudberries.candidatematch.infrastructure.repositories.embedding.CvEmbeddingRepository
import no.cloudberries.candidatematch.infrastructure.repositories.embedding.CvEmbeddingRepository.VectorSearchHit
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ConsultantSearchService(
    private val searchRepository: ConsultantSearchRepository,
    private val embeddingRepository: CvEmbeddingRepository,
    private val consultantRepository: no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository,
) {
    data class ConsultantSearchResultDto(
        val id: Long?,
        val userId: String,
        val name: String,
        val cvId: String,
        val skills: Set<Skill> = emptySet(),
        val similarity: Double? = null,
    )

    data class RelationalQuery(
        val name: String? = null,
        val skillsAll: Set<Skill> = emptySet(),
        val skillsAny: Set<Skill> = emptySet(),
        val minQualityScore: Int? = null,
        val onlyActiveCv: Boolean = false,
    )

    data class SemanticQuery(
        val text: String,
        val provider: String,
        val model: String,
        val topK: Int = 10,
    )

    fun searchRelational(query: RelationalQuery, pageable: Pageable): Page<ConsultantSearchResultDto> {
        val filters = ConsultantSearchRepository.RelationalFilters(
            name = query.name,
            skillsAll = query.skillsAll,
            skillsAny = query.skillsAny,
            minQualityScore = query.minQualityScore,
            onlyActiveCv = query.onlyActiveCv,
        )
        val page: Page<ConsultantEntity> = searchRepository.search(
            filters,
            pageable
        )
        return page.map { it.toDto(null) }
    }

    fun searchSemantic(
        queryVector: DoubleArray,
        provider: String,
        model: String,
        topK: Int = 10
    ): List<ConsultantSearchResultDto> {
        val hits: List<VectorSearchHit> = embeddingRepository.searchSimilar(
            queryVector,
            provider,
            model,
            topK
        )
        return hits.map { hit ->
            val c = consultantRepository.findByUserId(hit.userId)
            if (c != null) {
                ConsultantSearchResultDto(
                    id = c.id,
                    userId = c.userId,
                    name = c.name,
                    cvId = c.cvId,
                    skills = c.skills,
                    similarity = hit.similarity,
                )
            } else {
                ConsultantSearchResultDto(
                    id = null,
                    userId = hit.userId,
                    name = "",
                    cvId = hit.cvId,
                    skills = emptySet(),
                    similarity = hit.similarity,
                )
            }
        }
    }

    private fun ConsultantEntity.toDto(similarity: Double?): ConsultantSearchResultDto =
        ConsultantSearchResultDto(
            id = id,
            userId = userId,
            name = name,
            cvId = cvId,
            skills = skills,
            similarity = similarity,
        )
}
