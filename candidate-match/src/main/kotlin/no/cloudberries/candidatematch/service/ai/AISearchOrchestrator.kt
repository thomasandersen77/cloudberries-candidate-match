package no.cloudberries.candidatematch.service.ai

import mu.KotlinLogging
import no.cloudberries.candidatematch.config.AIChatConfig
import no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto
import no.cloudberries.candidatematch.dto.ai.*
import no.cloudberries.candidatematch.service.consultants.ConsultantSearchService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.*
import kotlin.system.measureTimeMillis

@Service
class AISearchOrchestrator(
    private val interpretationService: AIQueryInterpretationService,
    private val consultantSearchService: ConsultantSearchService,
    private val config: AIChatConfig
    // TODO: Add RAGContextService when implemented
    // TODO: Add AIGenerationService when implemented
) {
    private val logger = KotlinLogging.logger { }

    @Timed
    fun searchChat(request: ChatSearchRequest): ChatSearchResponse {
        val startTime = System.currentTimeMillis()
        val timings = mutableMapOf<String, Long>()

        logger.info { "Processing chat search: '${request.text}' (topK=${request.topK})" }

        // Step 1: Interpret the query
        val interpretation: QueryInterpretation
        val interpretationTime = measureTimeMillis {
            interpretation = interpretationService.interpretQuery(
                request.text,
                request.forceMode
            )
        }
        timings["interpretation"] = interpretationTime

        logger.info {
            "Query interpretation: route=${interpretation.route}, confidence=${interpretation.confidence.route}"
        }

        // Step 2: Execute search based on interpretation
        return try {
            when (interpretation.route) {
                SearchMode.STRUCTURED -> executeStructuredSearch(
                    interpretation,
                    request,
                    timings
                )

                SearchMode.SEMANTIC -> executeSemanticSearch(
                    interpretation,
                    request,
                    timings
                )

                SearchMode.HYBRID -> executeHybridSearch(
                    interpretation,
                    request,
                    timings
                )

                SearchMode.RAG -> executeRagSearch(
                    interpretation,
                    request,
                    timings
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Search execution failed, attempting fallback" }
            executeFallbackSearch(
                request,
                timings
            )
        }.also {
            val totalTime = System.currentTimeMillis() - startTime
            logger.info {
                "Chat search completed in ${totalTime}ms: mode=${it.mode}, " +
                        "resultCount=${it.results?.size ?: 0}, " +
                        "hasAnswer=${it.answer != null}"
            }
        }
    }

    private fun executeStructuredSearch(
        interpretation: QueryInterpretation,
        request: ChatSearchRequest,
        timings: MutableMap<String, Long>
    ): ChatSearchResponse {
        logger.debug { "Executing structured search" }

        if (interpretation.structured == null) {
            throw IllegalStateException("Structured search requires structured criteria")
        }

        val criteria = interpretation.structured.toRelationalSearchCriteria()
        val pageable = PageRequest.of(
            0,
            request.topK
        )

        val consultantPage: Page<ConsultantWithCvDto>
        val searchTime = measureTimeMillis {
            consultantPage = consultantSearchService.searchRelational(
                criteria,
                pageable
            )
        }
        timings["search"] = searchTime

        val searchResults = consultantPage.content.map { consultant ->
            SearchResult(
                consultantId = UUID.fromString(consultant.userId), // Use userId as UUID identifier
                name = consultant.name,
                score = calculateQualityScore(consultant),
                highlights = extractSkillHighlights(
                    consultant,
                    interpretation.structured
                ),
                meta = mapOf(
                    "cvCount" to consultant.cvs.size,
                    "skills" to getAllSkillsFromConsultant(consultant)
                )
            )
        }

        return ChatSearchResponse(
            mode = SearchMode.STRUCTURED,
            results = searchResults,
            answer = null,
            sources = null,
            latencyMs = timings.values.sum(),
            debug = createDebugInfo(
                interpretation,
                timings
            ),
            conversationId = request.conversationId
        )
    }

    private fun executeSemanticSearch(
        interpretation: QueryInterpretation,
        request: ChatSearchRequest,
        timings: MutableMap<String, Long>
    ): ChatSearchResponse {
        logger.debug { "Executing semantic search" }

        if (!config.semantic.enabled) {
            logger.warn { "Semantic search is disabled, falling back to structured search" }
            return executeFallbackSearch(
                request,
                timings
            )
        }

        if (interpretation.semanticText.isNullOrBlank()) {
            throw IllegalStateException("Semantic search requires semantic text")
        }

        // Create semantic search criteria
        val semanticCriteria = no.cloudberries.candidatematch.domain.consultant.SemanticSearchCriteria(
            text = interpretation.semanticText,
            provider = "GOOGLE_GEMINI", // Use configured provider
            model = config.models.embeddings,
            topK = request.topK,
            minQualityScore = null,
            onlyActiveCv = true
        )

        val consultantPage: Page<ConsultantWithCvDto>
        val searchTime = measureTimeMillis {
            consultantPage = consultantSearchService.searchSemantic(
                semanticCriteria,
                PageRequest.of(
                    0,
                    request.topK
                )
            )
        }
        timings["search"] = searchTime

        val searchResults = consultantPage.content.map { consultant ->
            val semanticText = interpretation.semanticText
            SearchResult(
                consultantId = UUID.fromString(consultant.userId),
                name = consultant.name,
                score = calculateQualityScore(consultant),
                highlights = listOf("Semantic match for: $semanticText"),
                meta = mapOf(
                    "cvCount" to consultant.cvs.size,
                    "semanticScore" to calculateQualityScore(consultant)
                )
            )
        }

        return ChatSearchResponse(
            mode = SearchMode.SEMANTIC,
            results = searchResults,
            answer = null,
            sources = null,
            latencyMs = timings.values.sum(),
            debug = createDebugInfo(
                interpretation,
                timings
            ),
            conversationId = request.conversationId
        )
    }

    private fun executeHybridSearch(
        interpretation: QueryInterpretation,
        request: ChatSearchRequest,
        timings: MutableMap<String, Long>
    ): ChatSearchResponse {
        logger.debug { "Executing hybrid search" }

        if (!config.hybrid.enabled) {
            logger.warn { "Hybrid search is disabled, falling back to structured search" }
            return executeStructuredSearch(
                interpretation,
                request,
                timings
            )
        }

        // Phase 1: Structured filtering with wider topK
        val structuredResults = if (interpretation.structured != null) {
            val widerTopK = minOf(
                request.topK * 3,
                100
            ) // Get more candidates for re-ranking
            val criteria = interpretation.structured.toRelationalSearchCriteria()
            val pageable = PageRequest.of(
                0,
                widerTopK
            )

            consultantSearchService.searchRelational(
                criteria,
                pageable
            ).content
        } else {
            emptyList()
        }

        // Phase 2: Re-rank with semantic similarity (TODO: implement)
        logger.warn { "Hybrid re-ranking not yet implemented, using structured results only" }

        val searchResults = structuredResults.take(request.topK).map { consultant ->
            SearchResult(
                consultantId = UUID.fromString(consultant.userId),
                name = consultant.name,
                score = calculateQualityScore(consultant),
                highlights = null,
                meta = mapOf("hybrid" to true)
            )
        }

        return ChatSearchResponse(
            mode = SearchMode.HYBRID,
            results = searchResults,
            answer = null,
            sources = null,
            latencyMs = timings.values.sum(),
            debug = createDebugInfo(
                interpretation,
                timings
            ),
            conversationId = request.conversationId
        )
    }

    private fun executeRagSearch(
        interpretation: QueryInterpretation,
        request: ChatSearchRequest,
        timings: MutableMap<String, Long>
    ): ChatSearchResponse {
        logger.debug { "Executing RAG search" }

        if (!config.rag.enabled) {
            logger.warn { "RAG is disabled, falling back to semantic search" }
            return executeSemanticSearch(
                interpretation,
                request,
                timings
            )
        }

        // TODO: Implement RAG search when RAGContextService and AIGenerationService are ready
        logger.warn { "RAG search not yet implemented, using fallback" }
        return ChatSearchResponse(
            mode = SearchMode.RAG,
            results = null,
            answer = "RAG functionality is not yet implemented. Please try a different search approach.",
            sources = null,
            latencyMs = timings.values.sum(),
            debug = createDebugInfo(
                interpretation,
                timings
            ),
            conversationId = request.conversationId
        )
    }

    private fun executeFallbackSearch(
        request: ChatSearchRequest,
        timings: MutableMap<String, Long>
    ): ChatSearchResponse {
        logger.debug { "Executing fallback search" }

        // Create basic semantic interpretation as fallback
        val fallbackInterpretation = QueryInterpretation(
            route = SearchMode.SEMANTIC,
            structured = null,
            semanticText = request.text,
            consultantName = null,
            question = null,
            confidence = ConfidenceScores(
                route = 0.3,
                extraction = 0.3
            )
        )

        return ChatSearchResponse(
            mode = SearchMode.SEMANTIC,
            results = emptyList(), // TODO: Implement basic search fallback
            answer = null,
            sources = null,
            latencyMs = timings.values.sum(),
            debug = createDebugInfo(
                fallbackInterpretation,
                timings
            ),
            conversationId = request.conversationId
        )
    }

    private fun calculateQualityScore(consultant: ConsultantWithCvDto): Double {
        // Simple quality score calculation based on available data
        val cvCount = consultant.cvs.size
        val skillCount = getAllSkillsFromConsultant(consultant).size

        // Use average quality score from CVs if available, otherwise use skill-based calculation
        val avgQualityScore = consultant.cvs
            .mapNotNull { cv -> cv.qualityScore }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.div(100.0) // Normalize to 0-1 range
            ?: (cvCount * 0.1 + skillCount * 0.01) // Fallback calculation

        // Ensure score is between 0 and 1
        return minOf(
            1.0,
            maxOf(
                0.0,
                avgQualityScore
            )
        )
    }

    private fun getAllSkillsFromConsultant(consultant: ConsultantWithCvDto): List<String> {
        return consultant.cvs
            .flatMap { cv -> cv.skillCategories }
            .flatMap { category -> category.skills }
            .mapNotNull { skill -> skill.name }
            .distinct()
    }

    private fun extractSkillHighlights(
        consultant: ConsultantWithCvDto,
        criteria: StructuredCriteria
    ): List<String> {
        val consultantSkills = getAllSkillsFromConsultant(consultant)
            .map { it.lowercase() }
            .toSet()

        val matchedSkills = mutableListOf<String>()

        // Add matched skillsAll
        criteria.skillsAll.forEach { requiredSkill ->
            if (consultantSkills.contains(requiredSkill.lowercase())) {
                matchedSkills.add("Has required skill: $requiredSkill")
            }
        }

        // Add matched skillsAny
        criteria.skillsAny.forEach { skill ->
            if (consultantSkills.contains(skill.lowercase())) {
                matchedSkills.add("Has skill: $skill")
            }
        }

        return matchedSkills
    }

    private fun createDebugInfo(
        interpretation: QueryInterpretation,
        timings: Map<String, Long>
    ): DebugInfo {
        return DebugInfo(
            interpretation = interpretation,
            timings = timings,
            extra = mapOf(
                "configProvider" to config.provider,
                "ragEnabled" to config.rag.enabled,
                "semanticEnabled" to config.semantic.enabled,
                "hybridEnabled" to config.hybrid.enabled
            )
        )
    }
}