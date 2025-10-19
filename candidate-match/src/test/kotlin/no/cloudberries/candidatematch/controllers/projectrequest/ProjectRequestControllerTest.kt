package no.cloudberries.candidatematch.controllers.projectrequest

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.CustomerProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.ProjectRequestRequirementEntity
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.RequirementPriority
import no.cloudberries.candidatematch.service.ProjectRequestService
import no.cloudberries.candidatematch.service.projectrequest.ProjectRequestAnalysisService
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ProjectRequestControllerTest {

    private val analysisService = mockk<ProjectRequestAnalysisService>()
    private val projectRequestService = mockk<ProjectRequestService>()
    private val projectMatchingService = mockk<no.cloudberries.candidatematch.matches.service.ProjectMatchingService>(relaxed = true)
    private val controller = ProjectRequestController(analysisService, projectRequestService, projectMatchingService)
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller).build()

    @Test
    fun `upload should analyze and return dto`() {
        val file = MockMultipartFile(
            "file",
            "req.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "dummy".toByteArray()
        )

        val req = CustomerProjectRequestEntity(
            id = 99L,
            customerName = "Imported",
            title = "Tittel",
            summary = "AI SUMMARY",
            originalFilename = "req.pdf",
            originalText = "...",
            requirements = emptyList(),
        )

        val reqs = listOf(
            ProjectRequestRequirementEntity(projectRequest = req, name = "Kotlin developer", details = null, priority = RequirementPriority.MUST),
            ProjectRequestRequirementEntity(projectRequest = req, name = "React", details = null, priority = RequirementPriority.SHOULD),
        )
        val agg = ProjectRequestAnalysisService.Aggregate(request = req, requirements = reqs)

        every { analysisService.analyzeAndStore(any(), any()) } returns agg

        mockMvc.perform(
            multipart("/project-requests/upload").file(file)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(99))
            .andExpect(jsonPath("$.summary").value("AI SUMMARY"))
            .andExpect(jsonPath("$.mustRequirements[0].name").value("Kotlin developer"))
            .andExpect(jsonPath("$.shouldRequirements[0].name").value("React"))
    }
}
