package no.cloudberries.candidatematch.controllers.consultants

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.cloudberries.candidatematch.service.consultants.ConsultantSearchService
import no.cloudberries.candidatematch.service.consultants.EmbeddingProviderInfo
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ConsultantSearchControllerTest {

    private val consultantSearchService = mockk<ConsultantSearchService>()
    private val controller = ConsultantController(
        consultantReadService = mockk(relaxed = true),
        consultantSearchService = consultantSearchService
    )
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `POST search should return search results`() {
        val request = RelationalSearchRequest(
            name = "John",
            skillsAll = listOf("KOTLIN"),
            skillsAny = listOf("JAVA"),
            minQualityScore = 80,
            onlyActiveCv = true,
            pagination = PaginationDto(page = 0, size = 10, sort = listOf("name,asc"))
        )

        val mockResult = PageImpl(
            listOf(
                ConsultantWithCvDto(
                    id = 1L,
                    userId = "user1",
                    name = "John Doe",
                    cvId = "cv1",
                    skills = listOf("KOTLIN", "JAVA"),
                    cvs = emptyList()
                )
            ),
            PageRequest.of(0, 10),
            1L
        )

        every { consultantSearchService.searchRelational(any(), any()) } returns mockResult

        mockMvc.perform(
            post("/api/consultants/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].name").value("John Doe"))
            .andExpect(jsonPath("$.content[0].userId").value("user1"))
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `POST search semantic should return semantic search results`() {
        val request = SemanticSearchRequest(
            text = "Senior Kotlin developer",
            provider = "GOOGLE_GEMINI",
            model = "text-embedding-004",
            topK = 5,
            pagination = PaginationDto(page = 0, size = 10)
        )

        val mockResult = PageImpl(
            listOf(
                ConsultantWithCvDto(
                    id = 1L,
                    userId = "user1",
                    name = "John Doe",
                    cvId = "cv1",
                    skills = listOf("KOTLIN"),
                    cvs = emptyList()
                )
            ),
            PageRequest.of(0, 10),
            1L
        )

        every { consultantSearchService.searchSemantic(any(), any()) } returns mockResult

        mockMvc.perform(
            post("/api/consultants/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].name").value("John Doe"))
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `GET embedding info should return provider information`() {
        val providerInfo = EmbeddingProviderInfo(
            enabled = true,
            provider = "GOOGLE_GEMINI",
            model = "text-embedding-004",
            dimension = 768
        )

        every { consultantSearchService.getEmbeddingProviderInfo() } returns providerInfo

        mockMvc.perform(get("/api/consultants/search/embedding-info"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.provider").value("GOOGLE_GEMINI"))
            .andExpect(jsonPath("$.model").value("text-embedding-004"))
            .andExpect(jsonPath("$.dimension").value(768))
    }

    @Test
    fun `POST search should return 400 for invalid criteria`() {
        every { consultantSearchService.searchRelational(any(), any()) } throws IllegalArgumentException("Invalid criteria")

        val request = RelationalSearchRequest(minQualityScore = 150)

        mockMvc.perform(
            post("/api/consultants/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST semantic search should return 503 when embedding provider disabled`() {
        every { consultantSearchService.searchSemantic(any(), any()) } throws IllegalStateException("Embedding provider disabled")

        val request = SemanticSearchRequest(text = "Senior developer")

        mockMvc.perform(
            post("/api/consultants/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().is5xxServerError)
    }
}
