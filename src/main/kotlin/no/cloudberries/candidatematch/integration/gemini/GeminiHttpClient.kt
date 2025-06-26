package no.cloudberries.candidatematch.integration.gemini

import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import no.cloudberries.candidatematch.integration.openai.OpenAIConfig
import org.springframework.stereotype.Service

@Service
class GeminiHttpClient(
    val geminiConfig: GeminiConfig
) {

    fun call(prompt: String): String {

        val client: Client = Client.builder().apiKey(geminiConfig.apiKey).build()

        val response: GenerateContentResponse? =
            client.models.generateContent(
                "gemini-2.0-flash",
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

