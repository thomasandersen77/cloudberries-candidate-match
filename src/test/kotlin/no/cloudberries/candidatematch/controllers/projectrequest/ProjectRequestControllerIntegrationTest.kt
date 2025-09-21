package no.cloudberries.candidatematch.controllers.projectrequest

import LiquibaseTestConfig
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import no.cloudberries.candidatematch.templates.ProjectRequestParams
import no.cloudberries.candidatematch.templates.ProjectRequestPromptTemplate
import no.cloudberries.candidatematch.templates.renderProjectRequestTemplate
import no.cloudberries.candidatematch.utils.PdfUtils
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockReset
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.ByteArrayInputStream
import java.io.File

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(LiquibaseTestConfig::class)
class ProjectRequestControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean(reset = MockReset.NONE)
    lateinit var aiAnalysisService: AIAnalysisService

    @Test
    fun `upload and fetch project request`() {
        val aiJson = """
            {
              "project_request": {
                "customer_name": "Politiet",
                "title": "Fullstack-utvikler",
                "summary": "Modernisering av fagsystem",
                "must_requirements": [ { "name": "Spring Boot", "details": "3+ år" } ],
                "should_requirements": [ { "name": "React", "details": "Fordel" } ]
              }
            }
        """.trimIndent()

        val pdf = File("src/test/resources/politiet/cv-mal.pdf").readBytes()
        val expectedPrompt = ByteArrayInputStream(pdf).use { input ->
            val text = PdfUtils.extractText(input)
            renderProjectRequestTemplate(
                ProjectRequestPromptTemplate.template,
                ProjectRequestParams(requestText = text)
            )
        }

        Mockito.`when`(
            aiAnalysisService.analyzeContent(
                expectedPrompt,
                AIProvider.GEMINI
            )
        ).thenReturn(
            AIResponse(
                aiJson,
                "gemini"
            )
        )

        val mockFile = MockMultipartFile(
            "file",
            "foresporsel.pdf",
            "application/pdf",
            pdf
        )

        val mvcResult = mockMvc.perform(
            multipart("/api/project-requests/upload")
                .file(mockFile)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "$.customerName",
                    equalTo("Politiet")
                )
            )
            .andExpect(
                jsonPath(
                    "$.mustRequirements",
                    hasSize<Int>(1)
                )
            )
            .andReturn()

        val responseBody = mvcResult.response.contentAsString
        val idRegex = Regex("\"id\"\\s*:\\s*(\\d+)")
        val id = idRegex.find(responseBody)?.groupValues?.get(1)?.toLong() ?: error("id not found")

        mockMvc.perform(
            get("/api/project-requests/$id")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "$.id",
                    equalTo(id.toInt())
                )
            )
            .andExpect(
                jsonPath(
                    "$.shouldRequirements",
                    hasSize<Int>(1)
                )
            )
    }
}