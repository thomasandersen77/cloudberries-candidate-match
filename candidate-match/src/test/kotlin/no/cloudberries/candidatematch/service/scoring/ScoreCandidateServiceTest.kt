package no.cloudberries.candidatematch.service.scoring

import io.mockk.every
import io.mockk.mockk
import no.cloudberries.ai.domain.AIResponse
import no.cloudberries.ai.port.AiContentGenerationPort
import no.cloudberries.ai.domain.DefaultAIResponse
import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import no.cloudberries.candidatematch.service.ai.AIContentAnalysisService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScoreCandidateServiceTest {

    private val aiContentGenerationPort: AiContentGenerationPort = mockk()
    private val aiAnalysisService: AIAnalysisService = AIContentAnalysisService(aiContentGenerationPort)
    private val service = ScoreCandidateService(aiAnalysisService)

    @Test
    fun `performCvScoring parses JSON with markdown blocks`() {
        val jsonResponse = """
            ```json
            {
              "name": "Thomas",
              "summary": "Excellent profile.",
              "strengths": ["Leadership", "Cloud"],
              "improvements": ["More Open Source"],
              "scoreBreakdown": {
                "structureAndReadability": {"score": 90, "justification": "Clear structure"},
                "contentAndRelevance": {"score": 85, "justification": "Very relevant"},
                "quantificationAndResults": {"score": 80, "justification": "Good results"},
                "technicalDepth": {"score": 95, "justification": "Deep technical knowledge"},
                "languageAndProfessionalism": {"score": 100, "justification": "Flawless"}
              },
              "scorePercentage": 90
            }
            ```
        """.trimIndent()

        every { aiContentGenerationPort.generateContent(any(), any()) } returns DefaultAIResponse(jsonResponse, "test-model")

        val result = service.performCvScoring(AIProvider.OLLAMA, "some cv content", "Thomas")

        assertEquals("Thomas", result.name)
        assertEquals(90, result.scorePercentage)
        assertEquals(2, result.strengths?.size)
    }

    @Test
    fun `performCvScoring parses plain JSON`() {
        val jsonResponse = """
            {
              "name": "Alice",
              "summary": "Strong profile.",
              "strengths": ["Java", "Spring Boot"],
              "improvements": [],
              "scoreBreakdown": {
                "structureAndReadability": {"score": 80, "justification": "Good"},
                "contentAndRelevance": {"score": 80, "justification": "Good"},
                "quantificationAndResults": {"score": 80, "justification": "Good"},
                "technicalDepth": {"score": 80, "justification": "Good"},
                "languageAndProfessionalism": {"score": 80, "justification": "Good"}
              },
              "scorePercentage": 80
            }
        """.trimIndent()

        every { aiContentGenerationPort.generateContent(any(), any()) } returns DefaultAIResponse(jsonResponse, "test-model")

        val result = service.performCvScoring(AIProvider.OLLAMA, "some cv content", "Alice")

        assertEquals("Alice", result.name)
        assertEquals(80, result.scorePercentage)
    }

    @Test
    fun `performCvScoring parses JSON with conversational text prefix`() {
        val jsonResponse = """
            Sure, here is the CV review for Alice:
            ```json
            {
              "name": "Alice",
              "summary": "Strong profile.",
              "strengths": ["Java"],
              "improvements": [],
              "scoreBreakdown": {
                "structureAndReadability": {"score": 80, "justification": "Good"},
                "contentAndRelevance": {"score": 80, "justification": "Good"},
                "quantificationAndResults": {"score": 80, "justification": "Good"},
                "technicalDepth": {"score": 80, "justification": "Good"},
                "languageAndProfessionalism": {"score": 80, "justification": "Good"}
              },
              "scorePercentage": 80
            }
            ```
            I hope this helps!
        """.trimIndent()

        every { aiContentGenerationPort.generateContent(any(), any()) } returns DefaultAIResponse(jsonResponse, "test-model")

        val result = service.performCvScoring(AIProvider.OLLAMA, "some cv content", "Alice")

        assertEquals("Alice", result.name)
        assertEquals(80, result.scorePercentage)
    }

    @Test
    fun `performCvScoring handles extra text after plain JSON`() {
        val jsonResponse = """
            {
              "name": "Bob",
              "summary": "Mid profile.",
              "strengths": ["C++"],
              "improvements": [],
              "scoreBreakdown": {
                "structureAndReadability": {"score": 50, "justification": "Ok"},
                "contentAndRelevance": {"score": 50, "justification": "Ok"},
                "quantificationAndResults": {"score": 50, "justification": "Ok"},
                "technicalDepth": {"score": 50, "justification": "Ok"},
                "languageAndProfessionalism": {"score": 50, "justification": "Ok"}
              },
              "scorePercentage": 50
            }
            This is extra text that might break things.
        """.trimIndent()

        every { aiContentGenerationPort.generateContent(any(), any()) } returns DefaultAIResponse(jsonResponse, "test-model")

        val result = service.performCvScoring(AIProvider.OLLAMA, "some cv content", "Bob")

        assertEquals("Bob", result.name)
        assertEquals(50, result.scorePercentage)
    }
}
