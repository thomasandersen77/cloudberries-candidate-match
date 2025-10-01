package no.cloudberries.candidatematch.controllers.consultants

import mu.KotlinLogging
import no.cloudberries.candidatematch.service.consultants.ConsultantReadService
import no.cloudberries.candidatematch.service.consultants.ConsultantSearchService
import no.cloudberries.candidatematch.service.consultants.EmbeddingProviderInfo
import no.cloudberries.candidatematch.domain.consultant.RelationalSearchCriteria
import no.cloudberries.candidatematch.domain.consultant.SemanticSearchCriteria
import no.cloudberries.candidatematch.dto.consultants.ConsultantSummaryDto
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// Only read operations from Flowcase via service. No create/update.

@RestController
@RequestMapping("/api/consultants")
class ConsultantController(
    private val consultantReadService: ConsultantReadService,
    private val consultantSearchService: ConsultantSearchService,
) {
    private val logger = KotlinLogging.logger { }

    @GetMapping
    @Timed
    fun list(
        @RequestParam(required = false) name: String?,
        pageable: Pageable
    ): Page<ConsultantSummaryDto> {
        logger.info { "GET /api/consultants name='${name ?: ""}' page=${pageable.pageNumber} size=${pageable.pageSize} sort='${pageable.sort.joinToString()}" }
        return consultantReadService.listConsultants(
            name,
            pageable
        )
    }
    
    /**
     * Relational search for consultants based on structured criteria
     */
    @PostMapping("/search")
    @Timed
    fun searchRelational(
        @RequestBody request: RelationalSearchRequest,
    ): ResponseEntity<Page<ConsultantWithCvDto>> {
        logger.info { "POST /api/consultants/search name='${request.name}' skillsAll=${request.skillsAll.size} skillsAny=${request.skillsAny.size} minQuality=${request.minQualityScore} onlyActiveCv=${request.onlyActiveCv} page=${request.pagination?.page ?: 0} size=${request.pagination?.size ?: 10}" }
        return try {
            val criteria = RelationalSearchCriteria(
                name = request.name,
                skillsAll = request.skillsAll,
                skillsAny = request.skillsAny,
                minQualityScore = request.minQualityScore,
                onlyActiveCv = request.onlyActiveCv
            )
            val pageable = request.pagination.toPageable()
            val result = consultantSearchService.searchRelational(criteria, pageable)
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            logger.info { "Relational search returned 400: ${e.message}" }
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.info { "Relational search returned 500: ${e.message}" }
            ResponseEntity.internalServerError().build()
        }
    }
    
    /**
     * Semantic search for consultants using natural language
     */
    @PostMapping("/search/semantic")
    @Timed
    fun searchSemantic(
        @RequestBody request: SemanticSearchRequest,
    ): ResponseEntity<Page<ConsultantWithCvDto>> {
        logger.info { "POST /api/consultants/search/semantic text='${request.text.take(80)}' provider=${request.provider} model=${request.model} topK=${request.topK} minQuality=${request.minQualityScore} onlyActiveCv=${request.onlyActiveCv} page=${request.pagination?.page ?: 0} size=${request.pagination?.size ?: 10}" }
        return try {
            val criteria = SemanticSearchCriteria(
                text = request.text,
                provider = request.provider,
                model = request.model,
                topK = request.topK,
                minQualityScore = request.minQualityScore,
                onlyActiveCv = request.onlyActiveCv
            )
            val pageable = request.pagination.toPageable()
            val result = consultantSearchService.searchSemantic(criteria, pageable)
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            logger.info { "Semantic search returned 400: ${e.message}" }
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            logger.info { "Semantic search returned 503: ${e.message}" }
            ResponseEntity.status(503).build()
        } catch (e: Exception) {
            logger.info { "Semantic search returned 500: ${e.message}" }
            ResponseEntity.internalServerError().build()
        }
    }
    
    /**
     * Gets information about the available embedding provider
     */
    @GetMapping("/search/embedding-info")
    @Timed
    fun getEmbeddingInfo(): EmbeddingProviderInfo {
        logger.info { "GET /api/consultants/search/embedding-info" }
        return consultantSearchService.getEmbeddingProviderInfo()
    }
}

/**
 * Simple pagination DTO carried in request bodies, avoiding Pageable in JSON
 */
data class PaginationDto(
    val page: Int = 0,
    val size: Int = 10,
    // sort entries like "name,asc" or "createdAt,desc"
    val sort: List<String> = emptyList()
)

private fun PaginationDto?.toPageable(): PageRequest {
    val p = this ?: PaginationDto()
    val orders = p.sort.mapNotNull { entry ->
        val parts = entry.split(',').map { it.trim() }
        when (parts.size) {
            1 -> Sort.Order.asc(parts[0])
            2 -> {
                val property = parts[0]
                val direction = parts[1]
                val dir = try {
                    Sort.Direction.fromString(direction)
                } catch (_: Exception) {
                    // fallback if invalid direction
                    Sort.Direction.ASC
                }
                Sort.Order(dir, property)
            }
            else -> null
        }
    }
    val sort = if (orders.isNotEmpty()) Sort.by(orders) else Sort.unsorted()
    return PageRequest.of(p.page, p.size, sort)
}

/**
 * Request DTO for relational search
 */
data class RelationalSearchRequest(
    val name: String? = null,
    val skillsAll: List<String> = emptyList(),
    val skillsAny: List<String> = emptyList(),
    val minQualityScore: Int? = null,
    val onlyActiveCv: Boolean = false,
    val pagination: PaginationDto? = null,
)

/**
 * Request DTO for semantic search
 */
data class SemanticSearchRequest(
    val text: String,
    val provider: String = "GOOGLE_GEMINI",
    val model: String = "text-embedding-004",
    val topK: Int = 10,
    val minQualityScore: Int? = null,
    val onlyActiveCv: Boolean = false,
    val pagination: PaginationDto? = null,
)
