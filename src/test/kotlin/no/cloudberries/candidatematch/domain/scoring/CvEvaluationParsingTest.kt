package no.cloudberries.candidatematch.domain.scoring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CvEvaluationParsingTest {

    @Test
    fun `CVEvaluation parses from JSON structure`() {
        val json = """
            {
              "name": "Jane Doe",
              "summary": "Strong backend developer",
              "strengths": ["Kotlin", "Spring"],
              "improvements": ["DevOps"],
              "scoreBreakdown": {
                "structureAndReadability": {"score": 8, "justification": "Clear structure"},
                "contentAndRelevance": {"score": 9, "justification": "Good fit"},
                "quantificationAndResults": {"score": 7, "justification": "Some metrics"},
                "technicalDepth": {"score": 8, "justification": "Deep knowledge"},
                "languageAndProfessionalism": {"score": 9, "justification": "Professional tone"}
              },
              "scorePercentage": 85
            }
        """.trimIndent()

        val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
        val dto = mapper.readValue(
            json,
            CVEvaluation::class.java
        )
        assertEquals(
            "Jane Doe",
            dto.name
        )
        assertEquals(
            85,
            dto.scorePercentage
        )
        assertEquals(
            9,
            dto.scoreBreakdown?.contentAndRelevance?.score
        )
    }
}
