package no.cloudberries.candidatematch.service
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.Requirement
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.domain.event.DomainEventPublisher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CandidateMatchingServiceTest {

    private val aiAnalysisService = mockk<AIAnalysisService>(relaxed = true)
    private val domainEventPublisher = mockk<DomainEventPublisher>(relaxed = true)
    private val candidateMatchingService = CandidateMatchingService(
        aiAnalysisService,
        domainEventPublisher
    )
    private val mapper = jacksonObjectMapper()

    @Test
    fun `should return candidate match response for openAI provider`() {
        val cv = "This is a CV."
        val request = "This is a request."
        val consultantName = "John Doe"
        val expectedResponse = CandidateMatchResponse(
            totalScore = "9.5",
            summary = "This is a summary.",
            requirements = mutableListOf(
                Requirement(
                    "Requirement 1",
                    "This is a comment.",
                    "10"
                )
            )
        )
        val responseJson = mapper.writeValueAsString(expectedResponse)

        every {
            aiAnalysisService.analyzeContent(
                content = any(String::class),
                AIProvider.OPENAI
            )
        } returns AIResponse(
            responseJson,
            "openai"
        )
        val result = candidateMatchingService.matchCandidate(
            aiProvider = AIProvider.OPENAI,
            cv = cv,
            request = request,
            consultantName = consultantName
        )

        assertEquals(
            expectedResponse,
            result
        )
    }

    @Test
    fun `should return candidate match response for Gemini provider`() {
        val cv = "This is a CV."
        val request = "This is a request."
        val consultantName = "John Doe"
        val expectedResponse = CandidateMatchResponse(
            totalScore = "8.5",
            summary = "This is a summary.",
            requirements = mutableListOf(
                Requirement(
                    "Requirement 1",
                    "This is a comment.",
                    "10"
                )
            )
        )
        val responseJson = mapper.writeValueAsString(expectedResponse)

        every {
            aiAnalysisService.analyzeContent(
                content = any(String::class),
                AIProvider.GEMINI
            )
        } returns AIResponse(
            responseJson,
            "gemini"
        )

        val result = candidateMatchingService.matchCandidate(
            aiProvider = AIProvider.GEMINI,
            cv = cv,
            request = request,
            consultantName = consultantName
        )

        assertEquals(
            expectedResponse,
            result
        )
    }
}
