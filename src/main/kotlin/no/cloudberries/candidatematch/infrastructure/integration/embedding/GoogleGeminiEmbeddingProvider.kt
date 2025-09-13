package no.cloudberries.candidatematch.infrastructure.integration.embedding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.embedding.EmbeddingProvider
import no.cloudberries.candidatematch.infrastructure.integration.gemini.GeminiConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Service

@Service
class GoogleGeminiEmbeddingProvider(
    private val geminiConfig: GeminiConfig,
    private val embeddingConfig: EmbeddingConfig,
) : EmbeddingProvider {

    private val logger = KotlinLogging.logger { }

    override val providerName: String = "GOOGLE_GEMINI"
    override val modelName: String = embeddingConfig.model
    override val dimension: Int = embeddingConfig.dimension

    override fun isEnabled(): Boolean = embeddingConfig.enabled

    /**
     * For nå genererer vi en deterministisk "pseudo-embedding" ved å hashe teksten.
     * Dette gjør at kodebasen kompilerer og kan testes uten tilgang til ekte embedding-endepunkt.
     * Når du ønsker å koble til Google Embeddings, erstatt implementasjonen her med et kall
     * via google-genai SDK eller HTTP, og returner den ekte vektoren (dimensjon = [dimension]).
     */
    override fun embed(text: String): DoubleArray {
        if (!isEnabled()) {
            logger.debug { "Embedding is disabled by configuration. Returning empty embedding." }
            return DoubleArray(0)
        }
        if (geminiConfig.apiKey.isBlank()) {
            logger.warn { "Gemini API key not configured; cannot fetch embeddings." }
            return DoubleArray(0)
        }
        val model = modelName.ifBlank { "text-embedding-004" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${'$'}model:embedContent"
        val mapper = jacksonObjectMapper()
        val payload: String = mapper.writeValueAsString(
            mapOf(
                "content" to mapOf(
                    "parts" to listOf(mapOf("text" to text))
                )
            )
        )
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(mediaType))
            .addHeader(
                "Content-Type",
                "application/json"
            )
            .addHeader(
                "Accept",
                "application/json"
            )
            .addHeader(
                "x-goog-api-key",
                geminiConfig.apiKey
            )
            .build()

        val client = httpClient
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.error { "Gemini embedContent failed: HTTP ${'$'}{response.code} - ${'$'}{response.message}" }
                return DoubleArray(0)
            }
            val bodyStr = response.body.string()
            return parseEmbedding(mapper.readTree(bodyStr))
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient()

    private fun parseEmbedding(node: JsonNode): DoubleArray {
        // Expected shape: { "embedding": { "values": [..] } }
        val valuesNode = when {
            node.has("embedding") && node.get("embedding").has("values") -> node.get("embedding").get("values")
            node.has("embeddings") && node.get("embeddings").isArray && node.get("embeddings").size() > 0 -> {
                val first = node.get("embeddings").get(0)
                if (first.has("values")) first.get("values") else null
            }

            else -> null
        }
        if (valuesNode == null || !valuesNode.isArray) {
            logger.error { "Could not find embedding values in Gemini response" }
            return DoubleArray(0)
        }
        val doubles = DoubleArray(valuesNode.size()) { idx -> valuesNode.get(idx).asDouble() }
        if (doubles.size != dimension) {
            logger.warn { "Embedding dimension mismatch: expected ${'$'}dimension, got ${'$'}{doubles.size}" }
        }
        return doubles
    }
}