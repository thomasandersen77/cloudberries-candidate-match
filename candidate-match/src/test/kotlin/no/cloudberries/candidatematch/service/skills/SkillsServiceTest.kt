package no.cloudberries.candidatematch.service.skills

import io.mockk.every
import io.mockk.mockk
import no.cloudberries.candidatematch.domain.candidate.ConsultantSkillInfo
import no.cloudberries.candidatematch.domain.candidate.SkillAggregate
import no.cloudberries.candidatematch.domain.candidate.SkillService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SkillsServiceTest {

    private val domainSkillService: SkillService = mockk()
    private val skillsService = SkillsService(skillService = domainSkillService)

    @Test
    fun `aggregates consultants per skill and sorts names`() {
        every { domainSkillService.aggregateSkillsAcrossConsultants(null) } returns listOf(
            SkillAggregate(
                skillName = "JAVA",
                consultantCount = 2,
                consultants = listOf(
                    ConsultantSkillInfo(userId = "u1", name = "Alice", cvId = "cv1", durationYears = null),
                    ConsultantSkillInfo(userId = "u2", name = "Bob",   cvId = "cv2", durationYears = null),
                )
            ),
            SkillAggregate(
                skillName = "KOTLIN",
                consultantCount = 1,
                consultants = listOf(
                    ConsultantSkillInfo("u1", "Alice", "cv1", null)
                )
            ),
            SkillAggregate(
                skillName = "REACT",
                consultantCount = 1,
                consultants = listOf(
                    ConsultantSkillInfo("u3", "Charlie", "cv3", null)
                )
            )
        )

        val result = skillsService.listSkills(null)

        // Expect three skill aggregates: JAVA (2), KOTLIN (1), REACT (1)
        val javaAgg = result.first { it.name == "JAVA" }
        assertEquals(2, javaAgg.konsulenter.size)
        assertEquals(listOf("Alice", "Bob"), javaAgg.konsulenter.map { it.name })

        val kotlinAgg = result.first { it.name == "KOTLIN" }
        assertEquals(1, kotlinAgg.konsulenter.size)
        assertEquals("Alice", kotlinAgg.konsulenter.first().name)

        val reactAgg = result.first { it.name == "REACT" }
        assertEquals(1, reactAgg.konsulenter.size)
        assertEquals("Charlie", reactAgg.konsulenter.first().name)
    }

    @Test
    fun `filters by provided skills`() {
        every { domainSkillService.aggregateSkillsAcrossConsultants(listOf("java")) } returns listOf(
            SkillAggregate(
                skillName = "JAVA",
                consultantCount = 1,
                consultants = listOf(ConsultantSkillInfo("u1", "Alice", "cv1", null))
            )
        )

        val result = skillsService.listSkills(listOf("java"))
        assertEquals(1, result.size)
        assertEquals("JAVA", result.first().name)
        assertEquals(1, result.first().konsulenter.size)
    }
}
