package no.cloudberries.candidatematch.domain.consultant

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SearchCriteriaTest {

    @Test
    fun `RelationalSearchCriteria validation should pass for valid criteria`() {
        val criteria = RelationalSearchCriteria(
            name = "John",
            skillsAll = listOf("KOTLIN", "SPRING"),
            skillsAny = listOf("JAVA", "REACT"),
            minQualityScore = 80,
            onlyActiveCv = true
        )

        val errors = criteria.validate()
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `RelationalSearchCriteria validation should fail for invalid quality score`() {
        val criteria = RelationalSearchCriteria(
            minQualityScore = 150
        )

        val errors = criteria.validate()
        assertEquals(1, errors.size)
        assertTrue(errors.contains("minQualityScore must be between 0 and 100"))
    }

    @Test
    fun `RelationalSearchCriteria validation should fail for blank skills`() {
        val criteria = RelationalSearchCriteria(
            skillsAll = listOf("KOTLIN", ""),
            skillsAny = listOf(" ", "JAVA")
        )

        val errors = criteria.validate()
        assertEquals(1, errors.size)
        assertTrue(errors.contains("Skills cannot be blank"))
    }

    @Test
    fun `RelationalSearchCriteria validation should fail for negative quality score`() {
        val criteria = RelationalSearchCriteria(
            minQualityScore = -10
        )

        val errors = criteria.validate()
        assertEquals(1, errors.size)
        assertTrue(errors.contains("minQualityScore must be between 0 and 100"))
    }

    @Test
    fun `SemanticSearchCriteria validation should pass for valid criteria`() {
        val criteria = SemanticSearchCriteria(
            text = "Senior Kotlin developer with Spring experience",
            provider = "GOOGLE_GEMINI",
            model = "text-embedding-004",
            topK = 10,
            minQualityScore = 80,
            onlyActiveCv = true
        )

        val errors = criteria.validate()
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `SemanticSearchCriteria validation should fail for blank text`() {
        val criteria = SemanticSearchCriteria(
            text = ""
        )

        val errors = criteria.validate()
        assertTrue(errors.contains("Search text cannot be blank"))
    }

    @Test
    fun `SemanticSearchCriteria validation should fail for invalid topK`() {
        val criteria = SemanticSearchCriteria(
            text = "Senior developer",
            topK = 0
        )

        val errors = criteria.validate()
        assertTrue(errors.contains("topK must be between 1 and 100"))
    }

    @Test
    fun `SemanticSearchCriteria validation should fail for topK too high`() {
        val criteria = SemanticSearchCriteria(
            text = "Senior developer",
            topK = 150
        )

        val errors = criteria.validate()
        assertTrue(errors.contains("topK must be between 1 and 100"))
    }

    @Test
    fun `SemanticSearchCriteria validation should fail for invalid quality score`() {
        val criteria = SemanticSearchCriteria(
            text = "Senior developer",
            minQualityScore = 150
        )

        val errors = criteria.validate()
        assertTrue(errors.contains("minQualityScore must be between 0 and 100"))
    }

    @Test
    fun `SemanticSearchCriteria validation should fail for blank provider`() {
        val criteria = SemanticSearchCriteria(
            text = "Senior developer",
            provider = ""
        )

        val errors = criteria.validate()
        assertTrue(errors.contains("Provider cannot be blank"))
    }

    @Test
    fun `SemanticSearchCriteria validation should fail for blank model`() {
        val criteria = SemanticSearchCriteria(
            text = "Senior developer",
            model = ""
        )

        val errors = criteria.validate()
        assertTrue(errors.contains("Model cannot be blank"))
    }

    @Test
    fun `SemanticSearchCriteria validation should collect multiple errors`() {
        val criteria = SemanticSearchCriteria(
            text = "",
            topK = 0,
            minQualityScore = 150,
            provider = "",
            model = ""
        )

        val errors = criteria.validate()
        assertEquals(5, errors.size)
        assertTrue(errors.contains("Search text cannot be blank"))
        assertTrue(errors.contains("topK must be between 1 and 100"))
        assertTrue(errors.contains("minQualityScore must be between 0 and 100"))
        assertTrue(errors.contains("Provider cannot be blank"))
        assertTrue(errors.contains("Model cannot be blank"))
    }

    @Test
    fun `RelationalSearchCriteria should use default values`() {
        val criteria = RelationalSearchCriteria()

        assertNull(criteria.name)
        assertTrue(criteria.skillsAll.isEmpty())
        assertTrue(criteria.skillsAny.isEmpty())
        assertNull(criteria.minQualityScore)
        assertFalse(criteria.onlyActiveCv)
    }

    @Test
    fun `SemanticSearchCriteria should use default values`() {
        val criteria = SemanticSearchCriteria(text = "test")

        assertEquals("test", criteria.text)
        assertEquals("GOOGLE_GEMINI", criteria.provider)
        assertEquals("text-embedding-004", criteria.model)
        assertEquals(10, criteria.topK)
        assertNull(criteria.minQualityScore)
        assertFalse(criteria.onlyActiveCv)
    }
}