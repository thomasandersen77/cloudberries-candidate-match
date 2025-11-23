package no.cloudberries.candidatematch.controllers.scoring

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.cloudberries.candidatematch.service.scoring.CvScoreAppService
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class CvScoreControllerTest {

    private val appService = mockk<CvScoreAppService>(relaxed = true)
    private val controller = CvScoreController(appService)
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    private val mapper = jacksonObjectMapper()

    @Test
    fun `getCvScore delegates to service`() {
        every { appService.getScore("u1") } returns CvScoreDto(
            candidateId = "u1",
            scorePercent = 80,
            summary = "solid",
            strengths = listOf("X"),
            potentialImprovements = listOf("Y")
        )
        mockMvc.perform(get("/cv-score/u1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.candidateId").value("u1"))
            .andExpect(jsonPath("$.scorePercent").value(80))
        verify { appService.getScore("u1") }
    }

    @Test
    fun `run single scoring`() {
        every { appService.scoreCandidate("u1") } returns CvScoreDto(
            candidateId = "u1",
            scorePercent = 90,
            summary = "great",
            strengths = emptyList(),
            potentialImprovements = emptyList()
        )
        mockMvc.perform(post("/cv-score/u1/run").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.scorePercent").value(90))
        verify { appService.scoreCandidate("u1") }
    }

    @Test
    fun `run all scoring`() {
        every { appService.scoreAll() } returns no.cloudberries.candidatematch.service.scoring.CvScoreAppService.ScoreAllResult(2)
        mockMvc.perform(post("/cv-score/run/all").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.processedCount").value(2))
        verify { appService.scoreAll() }
    }
}