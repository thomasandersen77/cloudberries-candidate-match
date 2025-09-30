package no.cloudberries.ai.rag

import org.springframework.stereotype.Component
import kotlin.math.max
import kotlin.math.min

/**
 * Very small, whitespace-based token chunker.
 * Not model-accurate tokenization, but good enough as a starter.
 */
@Component
class TokenChunker(
    private val defaultMaxTokens: Int = 400,
    private val defaultOverlapTokens: Int = 50
) {
    fun chunk(text: String, maxTokens: Int = defaultMaxTokens, overlapTokens: Int = defaultOverlapTokens): List<String> {
        if (text.isBlank()) return emptyList()
        val normalized = text.replace("\r", "\n").replace("\t", " ").replace(Regex("\n+"), "\n").trim()
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()

        val chunks = mutableListOf<String>()
        var start = 0
        val size = tokens.size
        val maxT = max(1, maxTokens)
        val overlap = max(0, min(overlapTokens, maxT - 1))

        while (true) {
            val end = min(start + maxT, size)
            val piece = tokens.subList(start, end).joinToString(" ")
            chunks.add(piece)
            if (end == size) break
            start = max(0, end - overlap)
            if (start >= size) break
        }
        return chunks
    }
}
