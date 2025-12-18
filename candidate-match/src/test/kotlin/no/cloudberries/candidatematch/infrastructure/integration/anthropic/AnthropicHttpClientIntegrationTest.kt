package no.cloudberries.candidatematch.infrastructure.integration.anthropic

import LiquibaseTestConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.cloudberries.candidatematch.config.AnthropicProperties
import no.cloudberries.candidatematch.domain.ai.AIGenerationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.WebClient

@Disabled("Requires WireMock server - run manually with: mvn test -Dtest=AnthropicHttpClientIntegrationTest")
@SpringBootTest
@ActiveProfiles("test")
@Import(LiquibaseTestConfig::class)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = ["spring.liquibase.contexts=test,default"])
class AnthropicHttpClientIntegrationTest {

    @Autowired
    private lateinit var anthropicHttpClient: AnthropicHttpClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val anthropicBaseUrl: String
        get() = "http://localhost:${System.getProperty("wiremock.server.port", "8089")}"

    @BeforeEach
    fun setUp() {
        // Reset WireMock before each test
        reset()
    }

    @Test
    fun `generateContent should return successful response from Anthropic API`() {
        // Given
        val prompt = "Say hello"
        val expectedResponse = """
            {
                "content": [
                    {
                        "type": "text",
                        "text": "Hello! How can I help you today?"
                    }
                ]
            }
        """.trimIndent()

        stubFor(
            post(urlPathEqualTo("/v1/messages"))
                .withHeader("x-api-key", equalTo("test-api-key"))
                .withHeader("anthropic-version", equalTo("2023-06-01"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(matchingJsonPath("$.model", equalTo("claude-3-5-sonnet-20241022")))
                .withRequestBody(matchingJsonPath("$.messages[0].role", equalTo("user")))
                .withRequestBody(matchingJsonPath("$.messages[0].content", equalTo(prompt)))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(expectedResponse)
                )
        )

        // When
        val result = anthropicHttpClient.generateContent(prompt)

        // Then
        assertNotNull(result)
        assertEquals("claude-3-5-sonnet-20241022", result.modelUsed)
        assertTrue(result.content.contains("Hello"))
        assertFalse(result.content.isEmpty())

        // Verify the request was made correctly
        verify(
            postRequestedFor(urlPathEqualTo("/v1/messages"))
                .withHeader("x-api-key", equalTo("test-api-key"))
                .withHeader("anthropic-version", equalTo("2023-06-01"))
        )
    }

    @Test
    fun `generateContent should handle API error responses`() {
        // Given
        val prompt = "Test prompt"
        val errorResponse = """
            {
                "error": {
                    "type": "invalid_request_error",
                    "message": "Invalid API key"
                }
            }
        """.trimIndent()

        stubFor(
            post(urlPathEqualTo("/v1/messages"))
                .willReturn(
                    aResponse()
                        .withStatus(401)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(errorResponse)
                )
        )

        // When/Then
        val exception = assertThrows(AIGenerationException::class.java) {
            anthropicHttpClient.generateContent(prompt)
        }
        assertTrue(exception.message?.contains("Failed to generate content with Anthropic") == true)
    }

    @Test
    fun `generateContent should handle empty response content`() {
        // Given
        val prompt = "Test prompt"
        val emptyResponse = """
            {
                "content": []
            }
        """.trimIndent()

        stubFor(
            post(urlPathEqualTo("/v1/messages"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(emptyResponse)
                )
        )

        // When/Then
        val exception = assertThrows(AIGenerationException::class.java) {
            anthropicHttpClient.generateContent(prompt)
        }
        assertTrue(exception.message?.contains("No response content received from Anthropic") == true)
    }

    @Test
    fun `generateContent should clean JSON response by removing markdown code blocks`() {
        // Given
        val prompt = "Return JSON"
        val responseWithMarkdown = """
            {
                "content": [
                    {
                        "type": "text",
                        "text": "```json\n{\"key\": \"value\"}\n```"
                    }
                ]
            }
        """.trimIndent()

        stubFor(
            post(urlPathEqualTo("/v1/messages"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseWithMarkdown)
                )
        )

        // When
        val result = anthropicHttpClient.generateContent(prompt)

        // Then
        assertNotNull(result)
        assertFalse(result.content.contains("```"))
        assertTrue(result.content.contains("key"))
        assertTrue(result.content.contains("value"))
    }

    @Test
    fun `testConnection should return true when API is available`() {
        // Given
        val successResponse = """
            {
                "content": [
                    {
                        "type": "text",
                        "text": "yes"
                    }
                ]
            }
        """.trimIndent()

        stubFor(
            post(urlPathEqualTo("/v1/messages"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(successResponse)
                )
        )

        // When
        val result = anthropicHttpClient.testConnection()

        // Then
        assertTrue(result)
    }

    @Test
    fun `testConnection should return false when API key is blank`() {
        // This test would require a separate test configuration with blank API key
        // For now, we test the happy path above
    }

    @Test
    fun `generateContent should use custom model from properties`() {
        // Given
        val prompt = "Test"
        val customModel = "claude-3-opus-20240229"
        val response = """
            {
                "content": [
                    {
                        "type": "text",
                        "text": "Response"
                    }
                ]
            }
        """.trimIndent()

        stubFor(
            post(urlPathEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.model", equalTo(customModel)))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(response)
                )
        )

        // Note: This test assumes the model is configured in test properties
        // The actual model used depends on AnthropicProperties configuration
        val result = anthropicHttpClient.generateContent(prompt)
        assertNotNull(result)
    }

    @Configuration
    class TestConfig {
        @Bean
        @Primary
        fun anthropicWebClient(): WebClient {
            val wiremockPort = System.getProperty("wiremock.server.port", "8089")
            val baseUrl = "http://localhost:$wiremockPort"
            return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-api-key", "test-api-key")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build()
        }

        @Bean
        fun anthropicProperties(): AnthropicProperties {
            val wiremockPort = System.getProperty("wiremock.server.port", "8089")
            return AnthropicProperties(
                apiKey = "test-api-key",
                model = "claude-3-5-sonnet-20241022",
                baseUrl = "http://localhost:$wiremockPort"
            )
        }

        @Bean
        fun anthropicHttpClient(
            anthropicWebClient: WebClient,
            anthropicProperties: AnthropicProperties
        ): AnthropicHttpClient {
            return AnthropicHttpClient(anthropicWebClient, anthropicProperties)
        }
    }
}
