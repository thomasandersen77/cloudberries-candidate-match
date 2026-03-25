package no.cloudberries.ai.infrastructure.integration.embedding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.cloudberries.ai.config.GeminiProperties
import no.cloudberries.ai.config.EmbeddingConfig
import no.cloudberries.ai.port.EmbeddingPort
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import kotlin.math.min

@Service
@ConditionalOnProperty(prefix = "embedding", name = ["provider"], havingValue = "GEMINI", matchIfMissing = true)
class GoogleGeminiEmbeddingProvider(
    private val geminiConfig: GeminiProperties,
    private val embeddingConfig: EmbeddingConfig,
) : EmbeddingPort {

    private val logger = KotlinLogging.logger { }

    private val maxPayloadBytes = 30000

    override val providerName: String = "GOOGLE_GEMINI"
    override val modelName: String = embeddingConfig.model
    override val dimension: Int = embeddingConfig.dimension

    override fun isEnabled(): Boolean = embeddingConfig.enabled

    override fun embed(text: String): DoubleArray {
        if (!isEnabled()) {
            logger.debug { "Embedding is disabled by configuration. Returning empty embedding." }
            return DoubleArray(0)
        }
        if (geminiConfig.apiKey.isBlank()) {
            logger.warn { "Gemini API key not configured; cannot fetch embeddings." }
            return DoubleArray(0)
        }

        val testPayload = createPayload(text)
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

    private fun createPayload(text: String): String =
        createPayload(text, task = SemanticTask.RETRIEVAL_DOCUMENT, title = null)

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

    private fun embedSingleText(text: String): DoubleArray {
        val model = modelName.ifBlank { "text-embedding-004" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:embedContent"
        val payload = createPayload(
            text = text,
            task = SemanticTask.SEMANTIC_SIMILARITY,
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

        val client = OkHttpClient()
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
        logger.info { "Processing ${chunks.size} chunks for large text" }

        val embeddings = chunks.mapNotNull { chunk ->
            try {
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
        if (embeddings.isEmpty()) return DoubleArray(0)

        val dim = embeddings.first().size
        val average = DoubleArray(dim)
        var validCount = 0

        for (embedding in embeddings) {
            if (embedding.size == dim) {
                for (i in 0 until dim) {
                    average[i] += embedding[i]
                }
                validCount++
            }
        }

        if (validCount == 0) return DoubleArray(0)

        for (i in 0 until dim) {
            average[i] /= validCount.toDouble()
        }

        return average
    }

    private fun chunkText(text: String): List<String> {
        if (text.isEmpty()) return listOf("")
        val targetBytes = 20000
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size <= targetBytes) return listOf(text)

        val chunks = ArrayList<String>()
        var start = 0
        val length = text.length

        while (start < length) {
            var end = min(start + targetBytes, length)
            end = adjustEndByBytes(text, start, end, targetBytes)
            val snapped = snapToBoundary(text, start, end)
            var chunkEnd = snapped.coerceIn(start + 1, length)
            if (chunkEnd <= start) chunkEnd = (start + 1).coerceAtMost(length)
            chunks.add(text.substring(start, chunkEnd))
            start = chunkEnd
        }
        return chunks
    }

    private fun adjustEndByBytes(s: String, start: Int, proposedEnd: Int, targetBytes: Int): Int {
        var end = proposedEnd
        if (s.substring(start, end).toByteArray(Charsets.UTF_8).size <= targetBytes) {
            while (end < s.length) {
                val nextEnd = (end + 1024).coerceAtMost(s.length)
                if (s.substring(start, nextEnd).toByteArray(Charsets.UTF_8).size > targetBytes) break
                end = nextEnd
            }
            return end
        }
        while (end > start + 1) {
            end -= ((end - start) / 2).coerceAtLeast(1)
            if (s.substring(start, end).toByteArray(Charsets.UTF_8).size <= targetBytes) {
                var fine = end
                while (fine < s.length) {
                    if (s.substring(start, fine + 1).toByteArray(Charsets.UTF_8).size > targetBytes) break
                    fine += 1
                }
                return fine
            }
        }
        return (start + 1).coerceAtMost(s.length)
    }

    private fun snapToBoundary(s: String, start: Int, end: Int): Int {
        if (end - start < 50) return end
        val window = s.substring(start, end)
        val sentence = window.lastIndexOf(". ")
        if (sentence >= 0) return start + sentence + 1
        val newline = window.lastIndexOf('\n')
        if (newline >= 0) return start + newline + 1
        val space = window.lastIndexOf(' ')
        if (space >= 0) return start + space + 1
        return end
    }

    private fun parseEmbedding(node: JsonNode): DoubleArray {
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
