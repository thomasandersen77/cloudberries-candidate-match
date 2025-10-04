package no.cloudberries.candidatematch.controllers.skills

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.cloudberries.candidatematch.controllers.consultants.ConsultantSummaryDto
import no.cloudberries.candidatematch.service.skills.SkillsService
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class SkillsControllerTest {

    private val skillsService = mockk<SkillsService>()
    private val skillsReadService = mockk<no.cloudberries.candidatematch.service.skills.SkillsReadService>()

    private val controller = SkillsController(skillsService, skillsReadService)
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    private val mapper = jacksonObjectMapper()

    @Test
    fun `should list skills with consultant counts`() {
        val javaConsultants = listOf(
            ConsultantSummaryDto("u1", "Alice", "alice@example.com", 1990, "cv1"),
            ConsultantSummaryDto("u2", "Bob", "bob@example.com", 1988, "cv2")
        )
        val kotlinConsultants = listOf(
            ConsultantSummaryDto("u1", "Alice", "alice@example.com", 1990, "cv1")
        )
        every { skillsService.listSkills(null) } returns listOf(
            SkillsService.SkillAggregate(name = "JAVA", konsulenter = javaConsultants),
            SkillsService.SkillAggregate(name = "KOTLIN", konsulenter = kotlinConsultants),
        )

        mockMvc.perform(
            get("/api/skills")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("JAVA"))
            .andExpect(jsonPath("$[0].konsulenterMedSkill").value(2))
            .andExpect(jsonPath("$[0].konsulenter[0].userId").value("u1"))
            .andExpect(jsonPath("$[1].name").value("KOTLIN"))
            .andExpect(jsonPath("$[1].konsulenterMedSkill").value(1))

        verify { skillsService.listSkills(null) }
    }

    @Test
    fun `should filter by repeated skill params`() {
        val javaConsultants = listOf(
            ConsultantSummaryDto("u2", "Bob", "bob@example.com", 1988, "cv2")
        )
        every { skillsService.listSkills(listOf("JAVA", "KOTLIN")) } returns listOf(
            SkillsService.SkillAggregate(name = "JAVA", konsulenter = javaConsultants)
        )

        mockMvc.perform(
            get("/api/skills")
                .param("skill", "JAVA", "KOTLIN")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("JAVA"))
            .andExpect(jsonPath("$[0].konsulenterMedSkill").value(1))

        verify { skillsService.listSkills(listOf("JAVA", "KOTLIN")) }
    }
}