package no.cloudberries.candidatematch.service
import io.mockk.every
import io.mockk.mockk
import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.domain.CandidateMatchResponse
import no.cloudberries.ai.domain.Requirement
import no.cloudberries.ai.port.CandidateMatchingPort
import no.cloudberries.candidatematch.domain.event.DomainEventPublisher
import no.cloudberries.candidatematch.service.matching.CandidateMatchingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CandidateMatchingServiceTest {

    private val candidateMatchingPort = mockk<CandidateMatchingPort>(relaxed = true)
    private val domainEventPublisher = mockk<DomainEventPublisher>(relaxed = true)
    private val candidateMatchingService = CandidateMatchingService(
        candidateMatchingPort,
        domainEventPublisher
    )

    @Test
    fun `should return candidate match response for openAI provider`() {
        val cv = "This is a CV."
        val request = "This is a request."
        val consultantName = "John Doe"
        val expectedResponse = CandidateMatchResponse(
            totalScore = "9.5",
            summary = "This is a summary.",
            requirements = listOf(
                Requirement(
                    name = "Requirement 1",
                    isMustHave = false,
                    score = "10",
                    comment = "This is a comment."
                )
            )
        )

        every {
            candidateMatchingPort.matchCandidate(
                cv = cv,
                request = request,
                consultantName = consultantName,
                provider = AIProvider.OPENAI
            )
        } returns expectedResponse

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
            requirements = listOf(
                Requirement(
                    name = "Requirement 1",
                    isMustHave = false,
                    score = "10",
                    comment = "This is a comment."
                )
            )
        )

        every {
            candidateMatchingPort.matchCandidate(
                cv = cv,
                request = request,
                consultantName = consultantName,
                provider = AIProvider.GEMINI
            )
        } returns expectedResponse

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

    @Test
    fun `should return candidate match response for Anthropic provider`() {
        val cv = "This is a CV."
        val request = "This is a request."
        val consultantName = "John Doe"
        val expectedResponse = CandidateMatchResponse(
            totalScore = "9.0",
            summary = "This is a summary.",
            requirements = listOf(
                Requirement(
                    name = "Requirement 1",
                    isMustHave = false,
                    score = "10",
                    comment = "This is a comment."
                )
            )
        )

        every {
            candidateMatchingPort.matchCandidate(
                cv = cv,
                request = request,
                consultantName = consultantName,
                provider = AIProvider.ANTHROPIC
            )
        } returns expectedResponse

        val result = candidateMatchingService.matchCandidate(
            aiProvider = AIProvider.ANTHROPIC,
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
    fun `should return candidate match response for Ollama provider`() {
        val cv = "This is a CV."
        val request = "This is a request."
        val consultantName = "John Doe"
        val expectedResponse = CandidateMatchResponse(
            totalScore = "7.5",
            summary = "This is a summary.",
            requirements = listOf(
                Requirement(
                    name = "Requirement 1",
                    isMustHave = false,
                    score = "10",
                    comment = "This is a comment."
                )
            )
        )

        every {
            candidateMatchingPort.matchCandidate(
                cv = cv,
                request = request,
                consultantName = consultantName,
                provider = AIProvider.OLLAMA
            )
        } returns expectedResponse

        val result = candidateMatchingService.matchCandidate(
            aiProvider = AIProvider.OLLAMA,
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
