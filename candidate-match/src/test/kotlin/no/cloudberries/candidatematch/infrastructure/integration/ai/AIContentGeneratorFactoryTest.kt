package no.cloudberries.candidatematch.infrastructure.integration.ai

import io.mockk.mockk
import no.cloudberries.candidatematch.domain.ai.AIContentGenerator
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.infrastructure.integration.anthropic.AnthropicHttpClient
import no.cloudberries.candidatematch.infrastructure.integration.gemini.GeminiHttpClient
import no.cloudberries.candidatematch.infrastructure.integration.openai.OpenAIHttpClient
import no.cloudberries.candidatematch.infrastructure.integration.ollama.OllamaHttpClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AIContentGeneratorFactoryTest {

    private lateinit var openAIHttpClient: OpenAIHttpClient
    private lateinit var geminiHttpClient: GeminiHttpClient
    private lateinit var ollamaHttpClient: OllamaHttpClient
    private lateinit var anthropicHttpClient: AnthropicHttpClient
    private lateinit var factory: AIContentGeneratorFactory

    @BeforeEach
    fun setUp() {
        openAIHttpClient = mockk<OpenAIHttpClient>(relaxed = true)
        geminiHttpClient = mockk<GeminiHttpClient>(relaxed = true)
        ollamaHttpClient = mockk<OllamaHttpClient>(relaxed = true)
        anthropicHttpClient = mockk<AnthropicHttpClient>(relaxed = true)
        factory = AIContentGeneratorFactory(
            openAIHttpClient,
            geminiHttpClient,
            ollamaHttpClient,
            anthropicHttpClient
        )
    }

    @Test
    fun `should return OpenAI client for OPENAI provider`() {
        val generator = factory.getGenerator(AIProvider.OPENAI)
        assertSame(openAIHttpClient, generator)
        assertTrue(generator is AIContentGenerator)
    }

    @Test
    fun `should return Gemini client for GEMINI provider`() {
        val generator = factory.getGenerator(AIProvider.GEMINI)
        assertSame(geminiHttpClient, generator)
        assertTrue(generator is AIContentGenerator)
    }

    @Test
    fun `should return Ollama client for OLLAMA provider`() {
        val generator = factory.getGenerator(AIProvider.OLLAMA)
        assertSame(ollamaHttpClient, generator)
        assertTrue(generator is AIContentGenerator)
    }

    @Test
    fun `should return Anthropic client for ANTHROPIC provider`() {
        val generator = factory.getGenerator(AIProvider.ANTHROPIC)
        assertSame(anthropicHttpClient, generator)
        assertTrue(generator is AIContentGenerator)
    }

    @Test
    fun `should return different clients for different providers`() {
        val openAIGenerator = factory.getGenerator(AIProvider.OPENAI)
        val geminiGenerator = factory.getGenerator(AIProvider.GEMINI)
        val ollamaGenerator = factory.getGenerator(AIProvider.OLLAMA)
        val anthropicGenerator = factory.getGenerator(AIProvider.ANTHROPIC)

        assertNotSame(openAIGenerator, geminiGenerator)
        assertNotSame(openAIGenerator, ollamaGenerator)
        assertNotSame(openAIGenerator, anthropicGenerator)
        assertNotSame(geminiGenerator, ollamaGenerator)
        assertNotSame(geminiGenerator, anthropicGenerator)
        assertNotSame(ollamaGenerator, anthropicGenerator)
    }
}
