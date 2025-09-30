package no.cloudberries.candidatematch.dto.ai

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.*

/**
 * Response from AI consultant search
 */
@Schema(description = "Response from AI-powered consultant search")
data class ChatSearchResponse(
    @Schema(description = "Search mode used", example = "structured")
    val mode: SearchMode,
    
    @Schema(description = "Search results (for structured/semantic/hybrid modes)")
    val results: List<SearchResult>? = null,
    
    @Schema(description = "Generated answer text (for RAG mode)", example = "Based on the consultant's CV...")
    val answer: String? = null,
    
    @Schema(description = "Sources used for RAG answers")
    val sources: List<RAGSource>? = null,
    
    @Schema(description = "Response time in milliseconds", example = "1250")
    val latencyMs: Long,
    
    @Schema(description = "Optional debug information")
    val debug: DebugInfo? = null,
    
    @Schema(description = "Conversation ID for follow-up queries")
    val conversationId: String? = null
)

/**
 * Individual search result
 */
@Schema(description = "Search result for a consultant")
data class SearchResult(
    @Schema(description = "Consultant ID")
    val consultantId: UUID,
    
    @Schema(description = "Consultant name", example = "Thomas Andersen")
    val name: String,
    
    @Schema(description = "Relevance score (0-1)", example = "0.87")
    val score: Double,
    
    @Schema(description = "Text highlights from matching")
    val highlights: List<String>? = null,
    
    @Schema(description = "Additional metadata")
    val meta: Map<String, Any>? = null
)

/**
 * RAG source citation
 */
@Schema(description = "Source citation for RAG answers")
data class RAGSource(
    @Schema(description = "Consultant ID")
    val consultantId: UUID,
    
    @Schema(description = "Consultant name")
    val consultantName: String,
    
    @Schema(description = "Chunk ID")
    val chunkId: UUID,
    
    @Schema(description = "Source text excerpt")
    val text: String,
    
    @Schema(description = "Relevance score", example = "0.92")
    val score: Double,
    
    @Schema(description = "CV section location", example = "Experience")
    val location: String?
)

/**
 * Debug information for development
 */
@Schema(description = "Debug information")
data class DebugInfo(
    @Schema(description = "Query interpretation details")
    val interpretation: QueryInterpretation? = null,
    
    @Schema(description = "Timing breakdown")
    val timings: Map<String, Long>? = null,
    
    @Schema(description = "Additional debug data")
    val extra: Map<String, Any>? = null
)