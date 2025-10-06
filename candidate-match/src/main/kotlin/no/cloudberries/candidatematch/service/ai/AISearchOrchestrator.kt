package no.cloudberries.candidatematch.service.ai

import mu.KotlinLogging
import no.cloudberries.candidatematch.config.AIChatConfig
import no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto
import no.cloudberries.candidatematch.domain.consultant.SemanticSearchCriteria
import no.cloudberries.candidatematch.dto.ai.*
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.service.consultants.ConsultantSearchService
import no.cloudberries.candidatematch.service.consultants.ConsultantWithCvService
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
    private val config: AIChatConfig,
    private val ragService: RAGService,
    private val conversationService: ConversationService,
    private val consultantRepository: ConsultantRepository,
    private val consultantWithCvService: ConsultantWithCvService
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

        // Step 2: Prepare context (history + optional consultant context)
        val history = request.conversationId?.let { conversationService.getConversationHistory(it) } ?: emptyList()
        val detected = detectTargetConsultant(request.text, history)
        val consultantContext = detected?.let { (_, userId) -> buildConsultantContext(userId) }

        // Step 3: Execute search based on interpretation
        val response = try {
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
        }
        return finalizeAndPersistConversation(request, response).also {
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

        var criteria = interpretation.structured.toRelationalSearchCriteria()
        // If we detected a specific consultant name, bias structured search by name
        if (criteria.name == null) {
            val localHistory = request.conversationId?.let { conversationService.getConversationHistory(it) } ?: emptyList()
            val nameFromDetection = detectTargetConsultant(request.text, localHistory)?.first
            if (!nameFromDetection.isNullOrBlank()) {
                criteria = criteria.copy(name = nameFromDetection)
            }
        }
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
consultantId = consultant.userId,
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

        // Build context (history + optional consultant context)
        val historyLocal = request.conversationId?.let { conversationService.getConversationHistory(it) } ?: emptyList()
        val detectedLocal = detectTargetConsultant(request.text, historyLocal)
        val consultantContextLocal = detectedLocal?.let { (_, userId) -> buildConsultantContext(userId) }

        // Create semantic search criteria (augment text with history/context)
        val augmentedText = augmentQuery(
            baseText = interpretation.semanticText,
            history = historyLocal,
            consultantContext = consultantContextLocal
        )
        val semanticCriteria = SemanticSearchCriteria(
            text = augmentedText,
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
consultantId = consultant.userId,
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

        // Phase 2: Re-rank with semantic similarity restricted to structured candidate set
        val allowedPairs = structuredResults.map { it.userId to it.cvId }
        val reRanked = if (allowedPairs.isNotEmpty()) {
            val base = interpretation.semanticText ?: request.text
            val historyLocal2 = request.conversationId?.let { conversationService.getConversationHistory(it) } ?: emptyList()
            val detectedLocal2 = detectTargetConsultant(request.text, historyLocal2)
            val consultantContextLocal2 = detectedLocal2?.let { (_, userId) -> buildConsultantContext(userId) }
            val augmented = augmentQuery(base, historyLocal2, consultantContextLocal2)
            consultantSearchService.reRankWithinCandidates(
                queryText = augmented,
                allowedPairs = allowedPairs,
                topK = request.topK
            )
        } else emptyList()

        val semanticWeight = config.hybrid.semanticWeight
        val qualityWeight = config.hybrid.qualityWeight

        val finalRanked = if (reRanked.isNotEmpty()) {
            reRanked.map { r ->
                val quality = calculateQualityScore(r.dto)
                val combined = semanticWeight * r.semanticScore + qualityWeight * quality
                val meta: Map<String, Any> = mapOf(
                    "hybrid" to true,
                    "semanticScore" to r.semanticScore,
                    "qualityScore" to quality
                )
                Triple(r.dto, combined, meta)
            }.sortedByDescending { it.second } // highest combined first
                .take(request.topK)
        } else {
            structuredResults.take(request.topK).map { dto ->
                val quality = calculateQualityScore(dto)
                val meta: Map<String, Any> = mapOf(
                    "hybrid" to true,
                    "qualityScore" to quality
                )
                Triple(dto, quality, meta)
            }
        }

        val searchResults = finalRanked.map { (dto, combined, meta) ->
            SearchResult(
consultantId = dto.userId,
                name = dto.name,
                score = combined,
                highlights = null,
                meta = meta
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
            conversationId = request.conversationId,
            scoring = ScoringInfo(
                semanticWeight = semanticWeight,
                qualityWeight = qualityWeight
            )
        )
    }

    private fun executeRagSearch(
        interpretation: QueryInterpretation,
        request: ChatSearchRequest,
        timings: MutableMap<String, Long>
    ): ChatSearchResponse {
        logger.info { "Executing RAG search" }

        if (!config.rag.enabled) {
            logger.warn { "RAG is disabled, falling back to semantic search" }
            return executeSemanticSearch(
                interpretation,
                request,
                timings
            )
        }

        // Check if we have consultant targeting information
        if (request.consultantId.isNullOrBlank()) {
            logger.warn { "RAG search requires consultantId, falling back to semantic search" }
            return executeSemanticSearch(
                interpretation,
                request,
                timings
            )
        }

        val cvId = request.cvId
        val question = interpretation.question ?: request.text

        return try {
            var ragResult: RAGService.RAGResult? = null
            val ragTime = measureTimeMillis {
                ragResult = ragService.processRAGQuery(
                    consultantId = request.consultantId,
                    cvId = cvId,
                    question = question,
                    conversationId = request.conversationId
                )
            }
            timings["rag"] = ragTime

            ChatSearchResponse(
                mode = SearchMode.RAG,
                results = null,
                answer = ragResult?.answer ?: "I encountered an error while processing your question about this consultant. Please try rephrasing your question or try again later.",
                sources = ragResult?.sources,
                latencyMs = timings.values.sum(),
                debug = createDebugInfo(
                    interpretation,
                    timings
                ),
                conversationId = ragResult?.conversationId ?: request.conversationId
            )
        } catch (e: Exception) {
            logger.error(e) { "RAG search failed for consultant ${request.consultantId}: ${e.message}" }
            ChatSearchResponse(
                mode = SearchMode.RAG,
                results = null,
                answer = "I encountered an error while processing your question about this consultant. Please try rephrasing your question or try again later.",
                sources = null,
                latencyMs = timings.values.sum(),
                debug = createDebugInfo(
                    interpretation,
                    timings,
                    mapOf("error" to e.message)
                ),
                conversationId = request.conversationId
            )
        }
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
        timings: Map<String, Long>,
        extraParams: Map<String, Any?> = emptyMap()
    ): DebugInfo {
        val baseExtra = mapOf(
            "configProvider" to config.provider,
            "ragEnabled" to config.rag.enabled,
            "semanticEnabled" to config.semantic.enabled,
            "hybridEnabled" to config.hybrid.enabled
        )
        
        return DebugInfo(
            interpretation = interpretation,
            timings = timings,
            extra = baseExtra + extraParams.filterValues { it != null } as Map<String, Any>
        )
    }

    private fun finalizeAndPersistConversation(
        request: ChatSearchRequest,
        response: ChatSearchResponse
    ): ChatSearchResponse {
        // Ensure a conversationId exists and return it to the client
        val convId = request.conversationId ?: UUID.randomUUID().toString()

        // RAG flow already persists conversation turns inside RAGService (with the built answer).
        // For non-RAG modes, persist a brief summary so follow-ups carry context.
        if (response.mode != SearchMode.RAG) {
            val summary = buildSummary(response)
            try {
                conversationService.addToConversation(convId, request.text, summary)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to persist conversation turn for $convId" }
            }
        }
        // Return response with conversationId attached
        return response.copy(conversationId = convId)
    }

    private fun buildSummary(response: ChatSearchResponse): String {
        return when (response.mode) {
            SearchMode.STRUCTURED -> summarizeResults("STRUCTURED", response.results)
            SearchMode.SEMANTIC -> summarizeResults("SEMANTIC", response.results)
            SearchMode.HYBRID -> summarizeResults("HYBRID", response.results)
            SearchMode.RAG -> response.answer ?: ""
        }
    }

    private fun summarizeResults(label: String, results: List<SearchResult>?): String {
        val list = results ?: emptyList()
        val count = list.size
        val top = list.take(3).joinToString(", ") { it.name }
        return if (count > 0) "$label search returned $count results. Top: $top" else "$label search returned no results."
    }

    private fun detectTargetConsultant(
        userText: String,
        history: List<ConversationService.ConversationTurn> = emptyList()
    ): Pair<String, String>? {
        // Try direct lookup by full text
        val page = org.springframework.data.domain.PageRequest.of(0, 1)
        val direct = consultantRepository.findByNameContainingIgnoreCase(userText, page)
        if (!direct.isEmpty) {
            val c = direct.content.first()
            return c.name to c.userId
        }
        // Try last turns' questions for a name fragment
        for (turn in history.asReversed()) {
            val guess = consultantRepository.findByNameContainingIgnoreCase(turn.question, page)
            if (!guess.isEmpty) {
                val c = guess.content.first()
                return c.name to c.userId
            }
        }
        return null
    }

    private fun buildConsultantContext(userId: String): String? {
        val cvs = consultantWithCvService.getCvsByUserId(userId, onlyActiveCv = true)
        val first = cvs.firstOrNull() ?: return null
        val skills = first.skillCategories.flatMap { cat -> cat.skills.mapNotNull { it.name } }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(15)
        if (skills.isEmpty()) return null
        return "Consultant userId=$userId skills: ${skills.joinToString(", ")}"
    }

    private fun augmentQuery(
        baseText: String?,
        history: List<ConversationService.ConversationTurn>,
        consultantContext: String?
    ): String {
        val base = baseText?.trim().orEmpty()
        val hist = history.takeLast(5).joinToString(" \n") { t ->
            val q = t.question.take(120)
            val a = t.answer.take(120)
            "Q:$q A:$a"
        }
        val ctx = consultantContext?.take(300)
        val parts = mutableListOf<String>()
        if (base.isNotEmpty()) parts += base
        if (hist.isNotEmpty()) parts += "History: $hist"
        if (!ctx.isNullOrEmpty()) parts += "Context: $ctx"
        return parts.joinToString(" \n")
    }
}
