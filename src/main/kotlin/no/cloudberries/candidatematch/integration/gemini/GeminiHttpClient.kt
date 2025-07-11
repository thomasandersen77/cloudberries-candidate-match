package no.cloudberries.candidatematch.integration.gemini

import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import org.springframework.stereotype.Service

@Service
class GeminiHttpClient(
    val geminiConfig: GeminiConfig
) {

    fun analyze(prompt: String): String {

        val client: Client = Client.builder().apiKey(geminiConfig.apiKey).build()

        val response: GenerateContentResponse? =
            client.models.generateContent(
                geminiConfig.model,
                prompt,
                null
            )

        val validJson = response?.text()
            ?.replace("```json", "")
            ?.replace("```", "")
        println(validJson)
        return validJson ?: throw RuntimeException("No response from Gemini")
    }
}

