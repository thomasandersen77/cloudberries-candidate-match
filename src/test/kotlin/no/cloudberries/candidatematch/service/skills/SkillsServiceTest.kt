package no.cloudberries.candidatematch.service.skills

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.mockk.every
import io.mockk.mockk
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SkillsServiceTest {

    private val consultantRepository: ConsultantRepository = mockk()
    private val skillsService = SkillsService(consultantRepository)

    @Test
    fun `aggregates consultants per skill and sorts names`() {
        val jsonFactory = JsonNodeFactory.instance
        val resume1 = jsonFactory.objectNode().put("email", "alice@example.com").put("bornYear", 1990)
        val resume2 = jsonFactory.objectNode().put("email", "bob@example.com").put("bornYear", 1988)
        val resume3 = jsonFactory.objectNode().put("email", "charlie@example.com").put("bornYear", 1992)

        val e1 = ConsultantEntity(
            id = null,
            userId = "u1",
            name = "Alice",
            cvId = "cv1",
            resumeData = resume1,
            skills = setOf(
                no.cloudberries.candidatematch.domain.candidate.Skill.JAVA,
                no.cloudberries.candidatematch.domain.candidate.Skill.KOTLIN,
            )
        )
        val e2 = ConsultantEntity(
            id = null,
            userId = "u2",
            name = "Bob",
            cvId = "cv2",
            resumeData = resume2,
            skills = setOf(no.cloudberries.candidatematch.domain.candidate.Skill.JAVA)
        )
        val e3 = ConsultantEntity(
            id = null,
            userId = "u3",
            name = "Charlie",
            cvId = "cv3",
            resumeData = resume3,
            skills = setOf(no.cloudberries.candidatematch.domain.candidate.Skill.REACT)
        )

        every { consultantRepository.findAllWithSkills() } returns listOf(e1, e2, e3)

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
        val jsonFactory = JsonNodeFactory.instance
        val resume1 = jsonFactory.objectNode().put("email", "alice@example.com").put("bornYear", 1990)
        val e1 = ConsultantEntity(
            id = null,
            userId = "u1",
            name = "Alice",
            cvId = "cv1",
            resumeData = resume1,
            skills = setOf(
                no.cloudberries.candidatematch.domain.candidate.Skill.JAVA,
                no.cloudberries.candidatematch.domain.candidate.Skill.KOTLIN,
            )
        )
        every { consultantRepository.findAllWithSkills() } returns listOf(e1)

        val result = skillsService.listSkills(listOf("java"))
        assertEquals(1, result.size)
        assertEquals("JAVA", result.first().name)
        assertEquals(1, result.first().konsulenter.size)
    }
}