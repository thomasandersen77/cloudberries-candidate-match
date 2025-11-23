package no.cloudberries.candidatematch.domain.candidate

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class SkillTest {

    @Test
    fun `should create basic skill with just name`() {
        val skill = Skill.of("Kotlin")
        
        assertEquals("Kotlin", skill.name)
        assertNull(skill.durationInYears)
    }

    @Test
    fun `should create skill with duration`() {
        val skill = Skill.withDuration("Java", 5)
        
        assertEquals("Java", skill.name)
        assertEquals(5, skill.durationInYears)
    }

    @Test
    fun `should create skill from project`() {
        val skill = Skill.fromProject("React", "E-commerce Platform", 2)
        
        assertEquals("React", skill.name)
        assertEquals(2, skill.durationInYears)
    }

    @Test
    fun `should trim skill names automatically`() {
        val skill = Skill.of("  Spring Boot  ")
        
        assertEquals("Spring Boot", skill.name)
    }

    @Test
    fun `should reject blank skill names`() {
        assertThrows<IllegalArgumentException> {
            Skill("", 1)
        }
        
        assertThrows<IllegalArgumentException> {
            Skill("   ", 2)
        }
    }

    @Test
    fun `should reject negative duration`() {
        assertThrows<IllegalArgumentException> {
            Skill("Kotlin", -1)
        }
    }

    @Test
    fun `should allow zero duration`() {
        val skill = Skill("TypeScript", 0)
        assertEquals(0, skill.durationInYears)
    }
}