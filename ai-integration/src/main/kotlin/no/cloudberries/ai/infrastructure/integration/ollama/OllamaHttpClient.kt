package no.cloudberries.ai.infrastructure.integration.ollama

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.cloudberries.ai.config.OllamaConfig
import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.domain.AIResponse
import no.cloudberries.ai.domain.DefaultAIResponse
import no.cloudberries.ai.port.AiContentGenerationPort
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class OllamaHttpClient(
    private val config: OllamaConfig
) {

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
                val errBody = resp.body?.string().orEmpty()
                throw RuntimeException(
                    "Error from Ollama API: ${resp.code} - ${resp.message}. Body: $errBody"
                )
            }
            val body = resp.body?.string().orEmpty()
            val node = mapper.readTree(body)
            return node["response"]?.asText()
                ?: throw IllegalStateException("Ollama returned unexpected payload: $body")
        }
    }

    fun generateContent(prompt: String): AIResponse {
        val content = generate(prompt)
        return DefaultAIResponse(
            content = content,
            modelUsed = config.model
        )
    }
}
