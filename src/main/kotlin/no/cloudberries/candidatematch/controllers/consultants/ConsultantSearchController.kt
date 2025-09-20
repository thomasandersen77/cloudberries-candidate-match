package no.cloudberries.candidatematch.controllers.consultants

import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.infrastructure.integration.embedding.EmbeddingConfig
import no.cloudberries.candidatematch.service.consultants.ConsultantSearchService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/consultants/search")
class ConsultantSearchController(
    private val service: ConsultantSearchService,
    private val embeddingConfig: EmbeddingConfig,
    private val embeddingProvider: no.cloudberries.candidatematch.domain.embedding.EmbeddingProvider,
) {

    data class RelationalSearchRequest(
        val name: String? = null,
        val skillsAll: Set<Skill> = emptySet(),
        val skillsAny: Set<Skill> = emptySet(),
        val minQualityScore: Int? = null,
        val onlyActiveCv: Boolean = false,
    )

    @PostMapping
    fun searchRelational(
        @RequestBody req: RelationalSearchRequest,
        @PageableDefault(size = 10) pageable: Pageable
    ): Page<ConsultantSearchService.ConsultantSearchResultDto> {
        val q = ConsultantSearchService.RelationalQuery(
            name = req.name,
            skillsAll = req.skillsAll,
            skillsAny = req.skillsAny,
            minQualityScore = req.minQualityScore,
            onlyActiveCv = req.onlyActiveCv,
        )
        return service.searchRelational(
            q,
            pageable
        )
    }

    data class SemanticSearchRequest(
        val text: String,
        val provider: String? = null,
        val model: String? = null,
        val topK: Int = 10,
    )

    @PostMapping("/semantic")
    fun searchSemantic(
        @RequestBody req: SemanticSearchRequest
    ): ResponseEntity<List<ConsultantSearchService.ConsultantSearchResultDto>> {
        val provider = req.provider ?: embeddingConfig.provider
        val model = req.model ?: embeddingConfig.model
        val vec = embeddingProvider.embed(req.text)
        if (vec.isEmpty()) {
            return ResponseEntity.badRequest().build()
        }
        val results = service.searchSemantic(
            vec,
            provider = provider,
            model = model,
            topK = req.topK
        )
        return ResponseEntity.ok(results)
    }
}
