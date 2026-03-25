package no.cloudberries.ai.infrastructure.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import no.cloudberries.ai.domain.DefaultAIResponse
import no.cloudberries.ai.dto.SearchMode
import no.cloudberries.ai.port.AiContentGenerationPort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QueryInterpretationAdapterTest {

    private val aiContentGenerationPort = mockk<AiContentGenerationPort>()
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private lateinit var adapter: QueryInterpretationAdapter

    @BeforeEach
    fun setUp() {
        adapter = QueryInterpretationAdapter(aiContentGenerationPort, objectMapper)
    }

    @Test
    fun `valid JSON maps to QueryInterpretation`() {
        val json = """
            {
              "route": "structured",
              "structured": {
                "skillsAll": ["kotlin", "spring"],
                "skillsAny": [],
                "roles": [],
                "minQualityScore": null,
                "locations": [],
                "availability": null,
                "publicSector": null,
                "customersAny": [],
                "industries": []
              },
              "semanticText": null,
              "consultantName": null,
              "question": null,
              "confidence": { "route": 0.95, "extraction": 0.9 }
            }
        """.trimIndent()

        every { aiContentGenerationPort.generateContent(any(), any()) } returns DefaultAIResponse(json, "test")

        val result = adapter.interpretQuery("Find Kotlin and Spring", null)

        assertThat(result.route).isEqualTo(SearchMode.STRUCTURED)
        assertThat(result.structured?.skillsAll).containsExactly("kotlin", "spring")
        assertThat(result.confidence.route).isEqualTo(0.95)
        assertThat(result.confidence.extraction).isEqualTo(0.9)
    }

    @Test
    fun `markdown fenced JSON still parses`() {
        val json = """
            ```json
            {
              "route": "semantic",
              "structured": null,
              "semanticText": "mentor for juniors",
              "consultantName": null,
              "question": null,
              "confidence": { "route": 0.9, "extraction": 0.8 }
            }
            ```
        """.trimIndent()

        every { aiContentGenerationPort.generateContent(any(), any()) } returns DefaultAIResponse(json, "test")

        val result = adapter.interpretQuery("experienced mentor", null)

        assertThat(result.route).isEqualTo(SearchMode.SEMANTIC)
        assertThat(result.semanticText).isEqualTo("mentor for juniors")
    }

    @Test
    fun `forceMode overrides AI route`() {
        val json = """
            {
              "route": "structured",
              "structured": null,
              "semanticText": null,
              "consultantName": null,
              "question": null,
              "confidence": { "route": 0.5, "extraction": 0.5 }
            }
        """.trimIndent()

        every { aiContentGenerationPort.generateContent(any(), any()) } returns DefaultAIResponse(json, "test")

        val result = adapter.interpretQuery("anything", SearchMode.SEMANTIC)

        assertThat(result.route).isEqualTo(SearchMode.SEMANTIC)
    }

    @Test
    fun `invalid JSON throws QueryInterpretationException`() {
        every { aiContentGenerationPort.generateContent(any(), any()) } returns DefaultAIResponse("not json", "test")

        assertThatThrownBy { adapter.interpretQuery("x", null) }
            .isInstanceOf(QueryInterpretationException::class.java)
            .hasMessageContaining("Failed to parse AI interpretation")
    }

    @Test
    fun `confidence values are clamped to 0..1`() {
        val json = """
            {
              "route": "rag",
              "structured": null,
              "semanticText": null,
              "consultantName": "A B",
              "question": "q",
              "confidence": { "route": 2.0, "extraction": -1.0 }
            }
        """.trimIndent()

        every { aiContentGenerationPort.generateContent(any(), any()) } returns DefaultAIResponse(json, "test")

        val result = adapter.interpretQuery("Tell me about A B", null)

        assertThat(result.confidence.route).isEqualTo(1.0)
        assertThat(result.confidence.extraction).isEqualTo(0.0)
    }
}
