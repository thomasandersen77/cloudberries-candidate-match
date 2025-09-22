package no.cloudberries.candidatematch.service
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.Requirement
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.domain.event.DomainEventPublisher
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import no.cloudberries.candidatematch.service.matching.CandidateMatchingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CandidateMatchingServiceTest {

    private val aiAnalysisService = mockk<AIAnalysisService>(relaxed = true)
    private val domainEventPublisher = mockk<DomainEventPublisher>(relaxed = true)
    private val consultantRepository = mockk<ConsultantRepository>(relaxed = true)
    private val candidateMatchingService = CandidateMatchingService(
        aiAnalysisService,
        domainEventPublisher,
        consultantRepository
    )
    private val mapper = jacksonObjectMapper()

    @Test
    fun `should return candidate match response for openAI provider`() {
        val cv = "This is a CV."
        val request = "This is a request."
        val consultantName = "John Doe"
        val expectedResponse = CandidateMatchResponse(
            totalScore = "9.5",
            summary = "This is a summary.",
            requirements = mutableListOf(
                Requirement(
                    "Requirement 1",
                    "This is a comment.",
                    "10"
                )
            )
        )
        val responseJson = mapper.writeValueAsString(expectedResponse)

        every {
            aiAnalysisService.analyzeContent(
                content = any(String::class),
                AIProvider.OPENAI
            )
        } returns AIResponse(
            responseJson,
            "openai"
        )
        val result = candidateMatchingService.matchCandidate(
            aiProvider = AIProvider.OPENAI,
            cv = cv,
            request = request,
            consultantName = consultantName
        )

        assertEquals(
            expectedResponse,
            result
        )
    }

    @Test
    fun `should return candidate match response for Gemini provider`() {
        val cv = "This is a CV."
        val request = "This is a request."
        val consultantName = "John Doe"
        val expectedResponse = CandidateMatchResponse(
            totalScore = "8.5",
            summary = "This is a summary.",
            requirements = mutableListOf(
                Requirement(
                    "Requirement 1",
                    "This is a comment.",
                    "10"
                )
            )
        )
        val responseJson = mapper.writeValueAsString(expectedResponse)

        every {
            aiAnalysisService.analyzeContent(
                content = any(String::class),
                AIProvider.GEMINI
            )
        } returns AIResponse(
            responseJson,
            "gemini"
        )

        val result = candidateMatchingService.matchCandidate(
            aiProvider = AIProvider.GEMINI,
            cv = cv,
            request = request,
            consultantName = consultantName
        )

        assertEquals(
            expectedResponse,
            result
        )
    }

    @Test
    fun `should find matches by skills with enum-based matching`() {
        val requiredSkills = listOf("java", "kotlin")
        val consultant1 = createTestConsultant(
            name = "John Doe",
            skills = setOf(Skill.JAVA, Skill.KOTLIN),
            resumeData = "CV content for John"
        )
        val consultant2 = createTestConsultant(
            name = "Jane Smith",
            skills = setOf(Skill.REACT, Skill.FRONTEND),
            resumeData = "CV content for Jane"
        )
        
        val expectedResponse = CandidateMatchResponse(
            totalScore = "8.5",
            summary = "Good match",
            requirements = listOf()
        )
        val responseJson = mapper.writeValueAsString(expectedResponse)

        every { consultantRepository.findAll() } returns listOf(consultant1, consultant2)
        every {
            aiAnalysisService.analyzeContent(
                content = any(String::class),
                AIProvider.GEMINI
            )
        } returns AIResponse(responseJson, "gemini")

        val result = candidateMatchingService.findMatchesBySkills(requiredSkills)

        assertEquals(1, result.size)
        assertEquals(expectedResponse, result.first())
    }

    @Test
    fun `should find matches by skills with resume text matching`() {
        val requiredSkills = listOf("spring")
        val consultant1 = createTestConsultant(
            name = "John Doe",
            skills = emptySet(),
            resumeData = "Experienced with Spring Boot and Spring Framework"
        )
        val consultant2 = createTestConsultant(
            name = "Jane Smith",
            skills = emptySet(),
            resumeData = "React and Angular developer"
        )
        
        val expectedResponse = CandidateMatchResponse(
            totalScore = "9.0",
            summary = "Excellent Spring match",
            requirements = listOf()
        )
        val responseJson = mapper.writeValueAsString(expectedResponse)

        every { consultantRepository.findAll() } returns listOf(consultant1, consultant2)
        every {
            aiAnalysisService.analyzeContent(
                content = any(String::class),
                AIProvider.GEMINI
            )
        } returns AIResponse(responseJson, "gemini")

        val result = candidateMatchingService.findMatchesBySkills(requiredSkills)

        assertEquals(1, result.size)
        assertEquals(expectedResponse, result.first())
    }

    @Test
    fun `should return empty list when no consultants match skills`() {
        val requiredSkills = listOf("cobol")
        val consultant1 = createTestConsultant(
            name = "John Doe",
            skills = setOf(Skill.JAVA, Skill.KOTLIN),
            resumeData = "Java and Kotlin developer"
        )
        
        every { consultantRepository.findAll() } returns listOf(consultant1)

        val result = candidateMatchingService.findMatchesBySkills(requiredSkills)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle multiple matching consultants`() {
        val requiredSkills = listOf("java")
        val consultant1 = createTestConsultant(
            name = "John Doe",
            skills = setOf(Skill.JAVA),
            resumeData = "Senior Java developer"
        )
        val consultant2 = createTestConsultant(
            name = "Jane Smith",
            skills = setOf(Skill.JAVA, Skill.KOTLIN),
            resumeData = "Full-stack Java developer"
        )
        
        val expectedResponse1 = CandidateMatchResponse(
            totalScore = "8.0",
            summary = "Good Java match",
            requirements = listOf()
        )
        val expectedResponse2 = CandidateMatchResponse(
            totalScore = "9.0",
            summary = "Excellent Java match",
            requirements = listOf()
        )
        val responseJson1 = mapper.writeValueAsString(expectedResponse1)
        val responseJson2 = mapper.writeValueAsString(expectedResponse2)

        every { consultantRepository.findAll() } returns listOf(consultant1, consultant2)
        every {
            aiAnalysisService.analyzeContent(
                content = any(String::class),
                AIProvider.GEMINI
            )
        } returnsMany listOf(
            AIResponse(responseJson1, "gemini"),
            AIResponse(responseJson2, "gemini")
        )

        val result = candidateMatchingService.findMatchesBySkills(requiredSkills)

        assertEquals(2, result.size)
        assertEquals(expectedResponse1, result[0])
        assertEquals(expectedResponse2, result[1])
    }

    @Test
    fun `should handle case-insensitive skill matching`() {
        val requiredSkills = listOf("JAVA", "kotlin")
        val consultant1 = createTestConsultant(
            name = "John Doe",
            skills = setOf(Skill.JAVA, Skill.KOTLIN),
            resumeData = "Java and Kotlin developer"
        )
        
        val expectedResponse = CandidateMatchResponse(
            totalScore = "8.5",
            summary = "Good match",
            requirements = listOf()
        )
        val responseJson = mapper.writeValueAsString(expectedResponse)

        every { consultantRepository.findAll() } returns listOf(consultant1)
        every {
            aiAnalysisService.analyzeContent(
                content = any(String::class),
                AIProvider.GEMINI
            )
        } returns AIResponse(responseJson, "gemini")

        val result = candidateMatchingService.findMatchesBySkills(requiredSkills)

        assertEquals(1, result.size)
        assertEquals(expectedResponse, result.first())
    }

    private fun createTestConsultant(
        name: String,
        skills: Set<Skill> = emptySet(),
        resumeData: String = "Default CV content"
    ): ConsultantEntity {
        val jsonNode: JsonNode = ObjectMapper().readTree(
            """{"content": "$resumeData"}"""
        )
        return ConsultantEntity(
            id = 1L,
            userId = "user-$name",
            name = name,
            cvId = "cv-$name",
            resumeData = jsonNode,
            skills = skills
        )
    }
}
