package no.cloudberries.candidatematch.controllers.matching

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.service.ai.AIService
import no.cloudberries.candidatematch.service.consultants.ConsultantReadService
import no.cloudberries.candidatematch.service.matching.CandidateMatchingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class MatchingControllerTest {

    private val aiService = mockk<AIService>(relaxed = true)
    private val consultantReadService = mockk<ConsultantReadService>(relaxed = true)
    private val candidateMatchingService = mockk<CandidateMatchingService>(relaxed = true)
    
    private val matchingController = MatchingController(
        aiService, 
        consultantReadService, 
        candidateMatchingService
    )
    
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(matchingController).build()
    private val mapper = jacksonObjectMapper()

    @Test
    fun `should return candidate matches for skills request`() {
        val skillsRequest = SkillsRequest(listOf("java", "kotlin"))
        val expectedResponse = listOf(
            CandidateMatchResponse(
                totalScore = "8.5",
                summary = "Good match for Java and Kotlin skills",
                requirements = listOf()
            )
        )

        every { 
            candidateMatchingService.findMatchesBySkills(
                requiredSkills = listOf("java", "kotlin"),
                aiProvider = AIProvider.GEMINI
            ) 
        } returns expectedResponse

        val result = mockMvc.perform(
            post("/api/matches/by-skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(skillsRequest))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$[0].totalScore").value("8.5"))
        .andExpect(jsonPath("$[0].summary").value("Good match for Java and Kotlin skills"))
        
        verify {
            candidateMatchingService.findMatchesBySkills(
                requiredSkills = listOf("java", "kotlin"),
                aiProvider = AIProvider.GEMINI
            )
        }
    }

    @Test
    fun `should return empty list when no matches found`() {
        val skillsRequest = SkillsRequest(listOf("cobol"))

        every { 
            candidateMatchingService.findMatchesBySkills(
                requiredSkills = listOf("cobol"),
                aiProvider = AIProvider.GEMINI
            ) 
        } returns emptyList()

        mockMvc.perform(
            post("/api/matches/by-skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(skillsRequest))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
        .andExpect(jsonPath("$").isEmpty)
        
        verify {
            candidateMatchingService.findMatchesBySkills(
                requiredSkills = listOf("cobol"),
                aiProvider = AIProvider.GEMINI
            )
        }
    }

    @Test
    fun `should handle multiple skills correctly`() {
        val skillsRequest = SkillsRequest(listOf("java", "spring", "react"))
        val expectedResponse = listOf(
            CandidateMatchResponse(
                totalScore = "9.0",
                summary = "Excellent full-stack match",
                requirements = listOf()
            ),
            CandidateMatchResponse(
                totalScore = "7.5",
                summary = "Good backend match",
                requirements = listOf()
            )
        )

        every { 
            candidateMatchingService.findMatchesBySkills(
                requiredSkills = listOf("java", "spring", "react"),
                aiProvider = AIProvider.GEMINI
            ) 
        } returns expectedResponse

        mockMvc.perform(
            post("/api/matches/by-skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(skillsRequest))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$[0].totalScore").value("9.0"))
        .andExpect(jsonPath("$[1].totalScore").value("7.5"))
        
        verify {
            candidateMatchingService.findMatchesBySkills(
                requiredSkills = listOf("java", "spring", "react"),
                aiProvider = AIProvider.GEMINI
            )
        }
    }
    
    @Test
    fun `direct method call should delegate to service correctly`() {
        val skillsRequest = SkillsRequest(listOf("kotlin"))
        val expectedResponse = listOf(
            CandidateMatchResponse(
                totalScore = "8.0",
                summary = "Kotlin expert",
                requirements = listOf()
            )
        )

        every { 
            candidateMatchingService.findMatchesBySkills(
                requiredSkills = listOf("kotlin"),
                aiProvider = AIProvider.GEMINI
            ) 
        } returns expectedResponse

        val result = matchingController.findMatchesBySkills(skillsRequest)
        
        assertEquals(expectedResponse, result)
        verify {
            candidateMatchingService.findMatchesBySkills(
                requiredSkills = listOf("kotlin"),
                aiProvider = AIProvider.GEMINI
            )
        }
    }
}