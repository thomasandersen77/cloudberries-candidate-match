package no.cloudberries.candidatematch.controllers.skills

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.cloudberries.candidatematch.controllers.skills.dto.SkillSummaryDto
import no.cloudberries.candidatematch.dto.consultants.ConsultantSummaryDto
import no.cloudberries.candidatematch.service.skills.SkillsService
import no.cloudberries.candidatematch.service.skills.SkillsReadService
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class SkillsNewEndpointsControllerTest {

    private val skillsService = mockk<SkillsService>()
    private val skillsReadService = mockk<SkillsReadService>()

    private val controller = SkillsController(skillsService, skillsReadService)
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    private val mapper = jacksonObjectMapper()

    @Test
    fun should_return_paged_skills_summary() {
        val page = PageImpl(
            listOf(
                SkillSummaryDto(name = "JAVA", consultantCount = 2),
                SkillSummaryDto(name = "KOTLIN", consultantCount = 1)
            ),
            PageRequest.of(0, 2),
            3
        )
        every { skillsReadService.listSkillSummary("ja", 0, 2, "consultantCount,desc") } returns page

        mockMvc.perform(
            get("/api/skills/summary")
                .param("q", "ja")
                .param("page", "0")
                .param("size", "2")
                .param("sort", "consultantCount,desc")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].name").value("JAVA"))
            .andExpect(jsonPath("$.content[0].consultantCount").value(2))
    }

    @Test
    fun should_return_paged_consultants_by_skill() {
        val page = PageImpl(
            listOf(
                ConsultantSummaryDto("u1", "Alice", "", 0, "cv1"),
                ConsultantSummaryDto("u2", "Bob", "", 0, "cv2")
            ),
            PageRequest.of(0, 2),
            2
        )
        every { skillsReadService.listConsultantsBySkill("JAVA", 0, 2, "name,asc") } returns page

        mockMvc.perform(
            get("/api/skills/JAVA/consultants")
                .param("page", "0")
                .param("size", "2")
                .param("sort", "name,asc")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].userId").value("u1"))
            .andExpect(jsonPath("$.content[1].name").value("Bob"))
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun should_return_skill_names_with_prefix() {
        every { skillsReadService.listSkillNames("Re", 5) } returns listOf("React", "Redis")

        mockMvc.perform(
            get("/api/skills/names")
                .param("prefix", "Re")
                .param("limit", "5")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value("React"))
            .andExpect(jsonPath("$[1]").value("Redis"))
    }
}