package no.cloudberries.candidatematch.infrastructure.integration.ollama

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.ai.AIContentGenerator
import no.cloudberries.candidatematch.domain.ai.AIResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class OllamaHttpClient(
    private val config: OllamaConfig
) : AIContentGenerator {

    private val logger = KotlinLogging.logger {}
    private val mapper = jacksonObjectMapper()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
            .build()
    }

    fun testConnection(): Boolean {
        return runCatching {
            val req = Request.Builder()
                .url("${config.baseUrl.trimEnd('/')}/api/version")
                .get()
                .build()
            client.newCall(req).execute().use { resp -> resp.isSuccessful }
        }.getOrElse {
            logger.warn(it) { "Failed to reach Ollama at ${config.baseUrl}" }
            false
        }
    }

    private fun generate(prompt: String): String {
        val payload = mapper.writeValueAsString(
            mapOf(
                "model" to config.model,
                "prompt" to prompt,
                "stream" to false
            )
        )

        val req = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/api/generate")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw RuntimeException("Error from Ollama API: ${resp.code} - ${resp.message}")
            }
            val body = resp.body?.string().orEmpty()
            val node = mapper.readTree(body)
            return node["response"]?.asText()
                ?: throw IllegalStateException("Ollama returned unexpected payload: $body")
        }
    }

    override fun generateContent(prompt: String): AIResponse {
        val content = generate(prompt)
        return AIResponse(
            content = content,
            modelUsed = config.model
        )
    }
}