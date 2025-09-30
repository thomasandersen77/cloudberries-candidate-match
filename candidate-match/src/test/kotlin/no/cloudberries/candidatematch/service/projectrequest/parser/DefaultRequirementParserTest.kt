package no.cloudberries.candidatematch.service.projectrequest.parser

import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.RequirementPriority
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultRequirementParserTest {

    private val parser = DefaultRequirementParser()

    @Test
    fun `parses MUST and SHOULD markers`() {
        val text = """
            Project Request
            MUST: Kotlin developer; Spring Boot experience
            SHOULD: React experience
        """.trimIndent()

        val result = parser.parse(text)
        assertEquals(3, result.size)
        val musts = result.filter { it.priority == RequirementPriority.MUST }.map { it.name }
        val shoulds = result.filter { it.priority == RequirementPriority.SHOULD }.map { it.name }
        assertTrue(musts.any { it.contains("Kotlin", ignoreCase = true) })
        assertTrue(musts.any { it.contains("Spring", ignoreCase = true) })
        assertTrue(shoulds.any { it.contains("React", ignoreCase = true) })
    }

    @Test
    fun `fallback parsing without markers`() {
        val text = """
            - Build pipelines
            Should know Docker
            Experience with Kotlin
        """.trimIndent()

        val result = parser.parse(text)
        assertEquals(3, result.size)
        assertEquals(RequirementPriority.SHOULD, result.first { it.name.contains("Docker", true) }.priority)
        assertEquals(RequirementPriority.MUST, result.first { it.name.contains("Kotlin", true) }.priority)
    }
}