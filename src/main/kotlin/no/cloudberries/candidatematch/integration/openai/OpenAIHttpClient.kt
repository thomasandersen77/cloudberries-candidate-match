package no.cloudberries.candidatematch.integration.openai

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class OpenAIHttpClient(
    private val config: OpenAIConfig
) {

    private val mapper = jacksonObjectMapper()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    fun analyze(prompt: String): String {
        // 1. Opprett en ny thread
        val threadId = createThread()

        // 2. Legg til melding i tråden
        postMessage(threadId, prompt)

        // 3. Start run
        val runId = startRun(threadId, config.assistantId)

        // 4. Vent til run er ferdig
        waitForRun(threadId, runId)

        // 5. Hent svaret
        return getMessages(threadId)
    }

    private fun createThread(): String {
        val response = post("https://api.openai.com/v1/threads", "{}")
        return mapper.readTree(response)["id"].asText()
    }

    private fun postMessage(threadId: String, content: String) {
        val json = mapper.writeValueAsString(
            mapOf(
                "role" to "user",
                "content" to content
            )
        )
        post("https://api.openai.com/v1/threads/$threadId/messages", json)
    }

    private fun startRun(threadId: String, assistantId: String): String {
        val json = mapper.writeValueAsString(mapOf("assistant_id" to assistantId))
        val response = post("https://api.openai.com/v1/threads/$threadId/runs", json)
        return mapper.readTree(response)["id"].asText()
    }

    private fun waitForRun(threadId: String, runId: String) {
        var status: String
        do {
            Thread.sleep(1000)
            val resp = get("https://api.openai.com/v1/threads/$threadId/runs/$runId")
            status = mapper.readTree(resp)["status"].asText()
            println("Status: $status")
            if(status == "failed") throw RuntimeException(
                "OpenAI Assistant feilet med melding: ${mapper.readTree(resp)["last_error"]["message"]}"
            )
        } while (status == "in_progress" || status == "queued")
    }

    private fun getMessages(threadId: String): String {
        val maxAttempts = 2
        val delayMillis = 500L

        repeat(maxAttempts) { attempt ->
            val resp = get("https://api.openai.com/v1/threads/$threadId/messages")

            val messages = mapper.readTree(resp)["data"]
            val reversed = messages.reversed()

            for (message in reversed) {
                val role = message["role"].asText()
                val content = message["content"].first()["text"]["value"].asText()
                if (role == "assistant") {
                    return content
                }
            }

            println("🔁 Forsøk ${attempt + 1}/$maxAttempts: fant ikke assistant-svar ennå, venter...")
            Thread.sleep(delayMillis)
        }

        return "❌ Fant ikke svar fra assistant i meldingsloggen etter $maxAttempts forsøk."
    }

    private fun post(url: String, body: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("OpenAI-Beta", "assistants=v2")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Feil fra OpenAI API: ${response.code} - ${response.message}")
            }
            return response.body!!.string()
        }
    }

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("OpenAI-Beta", "assistants=v2")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Feil fra OpenAI API: ${response.code} - ${response.message}")
            }
            return response.body?.string() ?: throw RuntimeException()
        }
    }
}