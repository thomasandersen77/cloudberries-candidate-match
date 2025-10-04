package no.cloudberries.candidatematch.infrastructure.services

import io.mockk.every
import io.mockk.mockk
import no.cloudberries.candidatematch.controllers.skills.dto.SkillSummaryDto
import no.cloudberries.candidatematch.dto.consultants.ConsultantSummaryDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate

class SkillsReadServiceImplTest {

    private val jdbcTemplate = mockk<JdbcTemplate>()
    private val service = SkillsReadServiceImpl(jdbcTemplate)

    @Test
    fun `listSkillSummary maps rows and counts`() {
        every { jdbcTemplate.query(any<String>(), any<Array<Any>>(), any<org.springframework.jdbc.core.RowMapper<SkillSummaryDto>>()) } returns listOf(
            SkillSummaryDto("JAVA", 2)
        )
        every { jdbcTemplate.queryForObject(any<String>(), any<Array<Any>>(), Long::class.java) } returns 1L

        val page = service.listSkillSummary("ja", 0, 10, "consultantCount,desc")
        assertEquals(1, page.totalElements)
        assertEquals(1, page.content.size)
        assertEquals("JAVA", page.content[0].name)
        assertEquals(2, page.content[0].consultantCount)
    }

    @Test
    fun `listConsultantsBySkill maps rows and counts`() {
        every { jdbcTemplate.query(any<String>(), any<Array<Any>>(), any<org.springframework.jdbc.core.RowMapper<ConsultantSummaryDto>>()) } returns listOf(
            ConsultantSummaryDto("u1", "Alice", "", 0, "cv1")
        )
        every { jdbcTemplate.queryForObject(any<String>(), any<Array<Any>>(), Long::class.java) } returns 1L

        val page = service.listConsultantsBySkill("JAVA", 0, 10, "name,asc")
        assertEquals(1, page.totalElements)
        assertEquals(1, page.content.size)
        assertEquals("u1", page.content[0].userId)
        assertEquals("Alice", page.content[0].name)
    }

    @Test
    fun `listSkillNames with prefix`() {
        every { jdbcTemplate.query(any<String>(), any<Array<Any>>(), any<org.springframework.jdbc.core.RowMapper<String>>()) } returns listOf("React", "Redis")

        val names = service.listSkillNames("Re", 5)
        assertEquals(listOf("React", "Redis"), names)
    }
}