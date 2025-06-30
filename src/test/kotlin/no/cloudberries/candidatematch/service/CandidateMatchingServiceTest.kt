package no.cloudberries.candidatematch.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.Requirement
import no.cloudberries.candidatematch.integration.AiProvider
import no.cloudberries.candidatematch.integration.gemini.GeminiHttpClient
import no.cloudberries.candidatematch.integration.openai.OpenAIHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CandidateMatchingServiceTest {

    private val openAIHttpClient: OpenAIHttpClient = Mockito.mock<OpenAIHttpClient>()
    private val geminiHttpClient: GeminiHttpClient = Mockito.mock<GeminiHttpClient>()
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

        Mockito.`when`(openAIHttpClient.analyze(Mockito.anyString())).thenReturn(responseJson)
        Mockito.`when`(geminiHttpClient.analyze(Mockito.anyString())).thenReturn(responseJson)

        val result = candidateMatchingService.matchCandidate(
            aiProvider = AiProvider.OPENAI,
            cv = cv,
            request = request,
            consultantName = consultantName
        )

        assertEquals(expectedResponse, result)
    }
}
