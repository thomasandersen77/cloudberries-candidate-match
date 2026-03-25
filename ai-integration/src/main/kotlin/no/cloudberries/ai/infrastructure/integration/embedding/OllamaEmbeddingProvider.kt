package no.cloudberries.ai.infrastructure.integration.embedding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.cloudberries.ai.config.EmbeddingConfig
import no.cloudberries.ai.config.OllamaConfig
import no.cloudberries.ai.port.EmbeddingPort
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(prefix = "embedding", name = ["provider"], havingValue = "OLLAMA")
class OllamaEmbeddingProvider(
    private val ollamaConfig: OllamaConfig,
    private val embeddingConfig: EmbeddingConfig,
) : EmbeddingPort {

    private val logger = KotlinLogging.logger { }
    private val mapper = jacksonObjectMapper()

    override val providerName: String = "OLLAMA"
    override val modelName: String = embeddingConfig.model.ifBlank { "bge-m3" }
    override val dimension: Int = embeddingConfig.dimension

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(ollamaConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(ollamaConfig.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(ollamaConfig.writeTimeoutSeconds, TimeUnit.SECONDS)
            .build()
    }

    override fun isEnabled(): Boolean = embeddingConfig.enabled

    override fun embed(text: String): DoubleArray {
        if (!isEnabled()) {
            logger.debug { "Embedding is disabled by configuration. Returning empty embedding." }
            return DoubleArray(0)
        }

        if (text.isBlank()) {
            logger.warn { "Cannot embed empty text" }
            return DoubleArray(0)
        }

        return try {
            embedSingleText(text)
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate embedding with Ollama" }
            DoubleArray(0)
        }
    }

    private fun embedSingleText(text: String): DoubleArray {
        val url = "${ollamaConfig.baseUrl.trimEnd('/')}/api/embed"
        
        val payload = mapper.writeValueAsString(
            mapOf(
                "model" to modelName,
                "input" to text
            )
        )

        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                logger.error { "Ollama embedContent failed: HTTP ${response.code} - $errorBody" }
                return DoubleArray(0)
            }

            val bodyStr = response.body?.string() ?: ""
            return parseEmbedding(mapper.readTree(bodyStr))
        }
    }

    private fun parseEmbedding(node: JsonNode): DoubleArray {
        // Ollama returns: {"model": "...", "embeddings": [[...]]}
        // or: {"embedding": [...]}
        
        val candidates = listOf(
            node.at("/embeddings/0"),
            node.at("/embedding")
        )
        
        val valuesNode = candidates.firstOrNull { it.isArray }

        if (valuesNode == null) {
            logger.error { "Could not find embedding values in Ollama response: ${node.toPrettyString()}" }
            return DoubleArray(0)
        }

        val doubles = DoubleArray(valuesNode.size()) { idx -> valuesNode.get(idx).asDouble() }
        
        if (doubles.size != dimension) {
            logger.warn { "Embedding dimension mismatch: expected $dimension, got ${doubles.size}. This is OK if model has different dimension." }
        }
        
        logger.debug { "Generated embedding with ${doubles.size} dimensions using model $modelName" }
        return doubles
    }
}
