package no.cloudberries.candidatematch.infrastructure.integration.embedding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.cloudberries.candidatematch.config.GeminiProperties
import no.cloudberries.candidatematch.domain.embedding.EmbeddingProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Service
import kotlin.math.min

@Service
class GoogleGeminiEmbeddingProvider(
    private val geminiConfig: GeminiProperties,
    private val embeddingConfig: EmbeddingConfig,
) : EmbeddingProvider {

    private val logger = KotlinLogging.logger { }

    // Conservative limit to stay well under 36KB payload limit
    private val maxChunkSize = 20000 // ~20KB per chunk
    private val maxPayloadBytes = 30000 // Conservative limit for entire payload

    override val providerName: String = "GOOGLE_GEMINI"
    override val modelName: String = embeddingConfig.model
    override val dimension: Int = embeddingConfig.dimension

    override fun isEnabled(): Boolean = embeddingConfig.enabled

    @no.cloudberries.candidatematch.utils.Timed
    override fun embed(text: String): DoubleArray {
        if (!isEnabled()) {
            logger.debug { "Embedding is disabled by configuration. Returning empty embedding." }
            return DoubleArray(0)
        }
        if (geminiConfig.apiKey.isBlank()) {
            logger.warn { "Gemini API key not configured; cannot fetch embeddings." }
            return DoubleArray(0)
        }

        // Check if text needs chunking
        val testPayload = createPayload(text) // defaults to RETRIEVAL_DOCUMENT
        val payloadSize = testPayload.toByteArray(Charsets.UTF_8).size

        return if (payloadSize > maxPayloadBytes) {
            logger.info { "Text too large ($payloadSize bytes), chunking into smaller pieces" }
            embedLargeText(text)
        } else {
            embedSingleText(text)
        }
    }

    enum class SemanticTask(val wire: String) {
        RETRIEVAL_DOCUMENT("RETRIEVAL_DOCUMENT"),
        RETRIEVAL_QUERY("RETRIEVAL_QUERY"),
        SEMANTIC_SIMILARITY("SEMANTIC_SIMILARITY")
    }

    // Backward-compatible overload
    private fun createPayload(text: String): String =
        createPayload(text, task = SemanticTask.RETRIEVAL_DOCUMENT, title = null)

    // New payload with semantic parameters
    private fun createPayload(
        text: String,
        task: SemanticTask,
        title: String? = null
    ): String {
        val mapper = jacksonObjectMapper()
        val root = mutableMapOf<String, Any>(
            "content" to mapOf(
                "parts" to listOf(mapOf("text" to text))
            ),
            "taskType" to task.wire
        )
        if (!title.isNullOrBlank()) {
            root["title"] = title
        }
        return mapper.writeValueAsString(root)
    }

    // Use query/document intent when embedding chunks or single text if needed:
    private fun embedSingleText(text: String): DoubleArray {
        val model = modelName.ifBlank { "text-embedding-004" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:embedContent"
        val payload = createPayload(
            text = text,
            task = SemanticTask.SEMANTIC_SIMILARITY, // or RETRIEVAL_QUERY for user queries
            title = null
        )

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(mediaType))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("x-goog-api-key", geminiConfig.apiKey)
            .build()

        val client = httpClient
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                logger.error { "Gemini embedContent failed: HTTP ${response.code} - $errorBody" }
                return DoubleArray(0)
            }
            val bodyStr = response.body?.string() ?: ""
            return parseEmbedding(jacksonObjectMapper().readTree(bodyStr))
        }
    }

    private fun embedLargeText(text: String): DoubleArray {
        val chunks = chunkText(text)
        logger.info { "Processing ${chunks.size} chunks for large text (first chunk bytes ~${chunks.firstOrNull()?.toByteArray(Charsets.UTF_8)?.size ?: 0})" }

        val embeddings = chunks.mapNotNull { chunk ->
            try {
                // Avoid logging actual chunk content; log sizes only
                logger.info { "Embedding chunk of length=${chunk.length}, bytes=${chunk.toByteArray(Charsets.UTF_8).size}" }
                val embedding = embedSingleText(chunk)
                if (embedding.isNotEmpty()) embedding else null
            } catch (e: Exception) {
                logger.warn(e) { "Failed to embed chunk, skipping" }
                null
            }
        }

        return if (embeddings.isNotEmpty()) {
            averageEmbeddings(embeddings)
        } else {
            logger.error { "Failed to embed any chunks from large text" }
            DoubleArray(0)
        }
    }

    private fun averageEmbeddings(embeddings: List<DoubleArray>): DoubleArray {
        if (embeddings.isEmpty()) {
            return DoubleArray(0)
        }

        val dimension = embeddings.first().size
        val average = DoubleArray(dimension)
        var validEmbeddingsCount = 0

        for (embedding in embeddings) {
            if (embedding.size == dimension) {
                for (i in 0 until dimension) {
                    average[i] += embedding[i]
                }
                validEmbeddingsCount++
            } else {
                logger.warn { "Inconsistent embedding dimensions, skipping one embedding. Expected $dimension, got ${embedding.size}" }
            }
        }

        if (validEmbeddingsCount == 0) {
            return DoubleArray(0)
        }

        for (i in 0 until dimension) {
            average[i] /= validEmbeddingsCount
        }

        return average
    }

    // Byte-aware, progress-guaranteed chunker
    private fun chunkText(text: String): List<String> {
        // Quick path
        if (text.isEmpty()) return listOf("")
        // Use a conservative byte budget per payload: leave headroom for JSON envelope
        val targetBytes = 20000 // ~20KB per chunk payload body
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size <= targetBytes) return listOf(text)

        val chunks = ArrayList<String>(bytes.size / targetBytes + 1)
        var start = 0
        val length = text.length

        while (start < length) {
            // Start with an optimistic end by chars, then adjust to byte limit
            var end = minOf(start + targetBytes, length)
            // Expand/shrink end to not exceed targetBytes in UTF-8
            end = adjustEndByBytes(text, start, end, targetBytes)

            // Try to snap to a natural boundary within [start, end]
            val snapped = snapToBoundary(text, start, end)
            var chunkEnd = snapped.coerceIn(start + 1, length) // ensure progress of at least 1 char

            // Final safety: if still no progress, advance by 1 char
            if (chunkEnd <= start) chunkEnd = (start + 1).coerceAtMost(length)

            chunks.add(text.substring(start, chunkEnd))
            start = chunkEnd
        }

        return chunks
    }

    // Ensure UTF-8 byte size <= targetBytes; returns an end index in chars
    private fun adjustEndByBytes(s: String, start: Int, proposedEnd: Int, targetBytes: Int): Int {
        var end = proposedEnd
        // Fast check
        var bytes = s.substring(start, end).toByteArray(Charsets.UTF_8)
        if (bytes.size <= targetBytes) {
            // Try to grow while under budget to reduce number of chunks
            while (end < s.length) {
                val nextEnd = (end + 1024).coerceAtMost(s.length)
                val candidate = s.substring(start, nextEnd).toByteArray(Charsets.UTF_8)
                if (candidate.size > targetBytes) break
                end = nextEnd
                bytes = candidate
                if (end == s.length) break
            }
            return end
        }
        // Shrink until under budget
        while (end > start + 1) {
            end -= ((end - start) / 2).coerceAtLeast(1) // binary-like shrink to speed up
            if (s.substring(start, end).toByteArray(Charsets.UTF_8).size <= targetBytes) {
                // Fine-tune forward to get closer to the limit without exceeding
                var fine = end
                while (fine < s.length) {
                    val candidate = s.substring(start, fine + 1).toByteArray(Charsets.UTF_8)
                    if (candidate.size > targetBytes) break
                    fine += 1
                }
                return fine
            }
        }
        // Worst case: single character
        return (start + 1).coerceAtMost(s.length)
    }

    // Prefer sentence/word boundaries if available within [start, end]
    private fun snapToBoundary(s: String, start: Int, end: Int): Int {
        if (end - start < 50) return end // small chunk, don't bother
        val window = s.substring(start, end)
        val sentence = window.lastIndexOf(". ")
        if (sentence >= 0) return start + sentence + 1
        val newline = window.lastIndexOf('\n')
        if (newline >= 0) return start + newline + 1
        val space = window.lastIndexOf(' ')
        if (space >= 0) return start + space + 1
        return end
    }

    private val httpClient: OkHttpClient = OkHttpClient()

    private fun parseEmbedding(node: JsonNode): DoubleArray {
        // Prefer JSON Pointer navigation for clarity and robustness
        val candidates = listOf(
            node.at("/embedding/values"),
            node.at("/embeddings/0/values")
        )
        val valuesNode = candidates.firstOrNull { it.isArray }

        if (valuesNode == null) {
            logger.error { "Could not find embedding values in Gemini response" }
            return DoubleArray(0)
        }

        val doubles = DoubleArray(valuesNode.size()) { idx -> valuesNode.get(idx).asDouble() }

        if (doubles.size != dimension) {
            logger.warn { "Embedding dimension mismatch: expected $dimension, got ${doubles.size}" }
        }

        return doubles
    }
}