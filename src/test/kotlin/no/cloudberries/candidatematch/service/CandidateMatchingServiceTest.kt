package no.cloudberries.candidatematch.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.Requirement
import no.cloudberries.candidatematch.integration.AiProvider
import no.cloudberries.candidatematch.integration.gemini.GeminiHttpClient
import no.cloudberries.candidatematch.integration.openai.OpenAIHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CandidateMatchingServiceTest {

    private val openAIHttpClient: OpenAIHttpClient = mockk<OpenAIHttpClient>(relaxed = true)
    private val geminiHttpClient: GeminiHttpClient = mockk<GeminiHttpClient>(relaxed = true)
    private val candidateMatchingService = CandidateMatchingService(openAIHttpClient, geminiHttpClient)
    private val mapper = jacksonObjectMapper()

    @Test
    fun `should return candidate match response for openAI provider`() {
        val cv = "This is a CV."
        val request = "This is a request."
        val consultantName = "John Doe"
        val expectedResponse = CandidateMatchResponse(
            totalScore = "10",
            summary = "This is a summary.",
            requirements = mutableListOf(Requirement("Requirement 1", "This is a comment.", "10"))
        )
        val responseJson = mapper.writeValueAsString(expectedResponse)

        every { openAIHttpClient.analyze(any(String::class)) } returns responseJson

        val result = candidateMatchingService.matchCandidate(
            aiProvider = AiProvider.OPENAI,
            cv = cv,
            request = request,
            consultantName = consultantName
        )

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `should return candidate match response for Gemini provider`() {
        val cv = "This is a CV."
        val request = "This is a request."
        val consultantName = "John Doe"
        val expectedResponse = CandidateMatchResponse(
            totalScore = "10",
            summary = "This is a summary.",
            requirements = mutableListOf(Requirement("Requirement 1", "This is a comment.", "10"))
        )
        val responseJson = mapper.writeValueAsString(expectedResponse)

        every { geminiHttpClient.analyze(prompt = any(String::class)) } returns responseJson

        val result = candidateMatchingService.matchCandidate(
            aiProvider = AiProvider.GEMINI,
            cv = cv,
            request = request,
            consultantName = consultantName
        )

        assertEquals(expectedResponse, result)
    }
}
