package no.cloudberries.candidatematch.service.consultants

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.cloudberries.candidatematch.controllers.consultants.ConsultantCvDto
import no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto
import no.cloudberries.candidatematch.domain.consultant.RelationalSearchCriteria
import no.cloudberries.candidatematch.domain.consultant.SemanticSearchCriteria
import no.cloudberries.candidatematch.domain.embedding.EmbeddingProvider
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantFlatView
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantSearchRepository
import no.cloudberries.candidatematch.infrastructure.repositories.SemanticSearchResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class ConsultantSearchServiceTest {

    private val consultantSearchRepository = mockk<ConsultantSearchRepository>()
    private val embeddingProvider = mockk<EmbeddingProvider>()
    private val cvDataAggregationService = mockk<CvDataAggregationService>()
    
    private val consultantSearchService = ConsultantSearchService(
        consultantSearchRepository,
        embeddingProvider,
        cvDataAggregationService
    )

    @Test
    fun `searchRelational should return consultants matching criteria`() {
        val criteria = RelationalSearchCriteria(
            name = "John",
            skillsAll = listOf("KOTLIN"),
            skillsAny = listOf("JAVA"),
            minQualityScore = 80,
            onlyActiveCv = true
        )
        val pageable = PageRequest.of(0, 10)
        
        val mockConsultant = mockk<ConsultantFlatView> {
            every { getId() } returns 1L
            every { getUserId() } returns "user1"
            every { getName() } returns "John Doe"
            every { getCvId() } returns "cv1"
        }
        
        val consultantPage = PageImpl(listOf(mockConsultant), pageable, 1)
        every { consultantSearchRepository.findByRelationalCriteria(criteria, pageable) } returns consultantPage
        every { cvDataAggregationService.aggregateCvData(listOf(1L), true) } returns mapOf(
            1L to listOf(
                ConsultantCvDto(
                    id = 1L,
                    versionTag = "v1",
                    qualityScore = 85,
                    active = true,
                    keyQualifications = emptyList(),
                    education = emptyList(),
                    workExperience = emptyList(),
                    projectExperience = emptyList(),
                    certifications = emptyList(),
                    courses = emptyList(),
                    languages = emptyList(),
                    skillCategories = emptyList(),
                    attachments = emptyList(),
                    industries = emptyList()
                )
            )
        )

        val result = consultantSearchService.searchRelational(criteria, pageable)

        assertEquals(1, result.content.size)
        assertEquals("John Doe", result.content[0].name)
        assertEquals("user1", result.content[0].userId)
        verify { consultantSearchRepository.findByRelationalCriteria(criteria, pageable) }
        verify { cvDataAggregationService.aggregateCvData(listOf(1L), true) }
    }

    @Test
    fun `searchRelational should throw exception for invalid criteria`() {
        val invalidCriteria = RelationalSearchCriteria(
            minQualityScore = 150 // Invalid score > 100
        )
        val pageable = PageRequest.of(0, 10)

        assertThrows<IllegalArgumentException> {
            consultantSearchService.searchRelational(invalidCriteria, pageable)
        }
    }

    @Test
    fun `searchSemantic should return consultants matching text`() {
        val criteria = SemanticSearchCriteria(
            text = "Senior Kotlin developer",
            provider = "GOOGLE_GEMINI",
            model = "text-embedding-004",
            topK = 5
        )
        val pageable = PageRequest.of(0, 10)
        val searchEmbedding = doubleArrayOf(0.1, 0.2, 0.3)
        
        every { embeddingProvider.isEnabled() } returns true
        every { embeddingProvider.providerName } returns "GOOGLE_GEMINI"
        every { embeddingProvider.modelName } returns "text-embedding-004"
        every { embeddingProvider.embed("Senior Kotlin developer") } returns searchEmbedding
        
        val semanticResults = listOf(
            SemanticSearchResult(
                id = 1L,
                userId = "user1",
                name = "John Doe",
                cvId = "cv1",
                qualityScore = 85,
                distance = 0.1
            )
        )
        
        every { 
            consultantSearchRepository.findBySemanticSimilarity(
                searchEmbedding, 
                "GOOGLE_GEMINI", 
                "text-embedding-004", 
                5, 
                null, 
                false
            ) 
        } returns semanticResults
        
        every { cvDataAggregationService.aggregateCvData(listOf(1L), false) } returns mapOf(
            1L to listOf(
                ConsultantCvDto(
                    id = 1L,
                    versionTag = "v1",
                    qualityScore = 85,
                    active = true,
                    keyQualifications = emptyList(),
                    education = emptyList(),
                    workExperience = emptyList(),
                    projectExperience = emptyList(),
                    certifications = emptyList(),
                    courses = emptyList(),
                    languages = emptyList(),
                    skillCategories = emptyList(),
                    attachments = emptyList(),
                    industries = emptyList()
                )
            )
        )

        val result = consultantSearchService.searchSemantic(criteria, pageable)

        assertEquals(1, result.content.size)
        assertEquals("John Doe", result.content[0].name)
        assertEquals("user1", result.content[0].userId)
        
        verify { embeddingProvider.embed("Senior Kotlin developer") }
        verify { 
            consultantSearchRepository.findBySemanticSimilarity(
                searchEmbedding, 
                "GOOGLE_GEMINI", 
                "text-embedding-004", 
                5, 
                null, 
                false
            ) 
        }
    }

    @Test
    fun `searchSemantic should throw exception when embedding provider disabled`() {
        val criteria = SemanticSearchCriteria(text = "Senior Kotlin developer")
        val pageable = PageRequest.of(0, 10)
        
        every { embeddingProvider.isEnabled() } returns false

        assertThrows<IllegalStateException> {
            consultantSearchService.searchSemantic(criteria, pageable)
        }
    }

    @Test
    fun `searchSemantic should throw exception for provider mismatch`() {
        val criteria = SemanticSearchCriteria(
            text = "Senior Kotlin developer",
            provider = "OPENAI",
            model = "text-embedding-ada-002"
        )
        val pageable = PageRequest.of(0, 10)
        
        every { embeddingProvider.isEnabled() } returns true
        every { embeddingProvider.providerName } returns "GOOGLE_GEMINI"
        every { embeddingProvider.modelName } returns "text-embedding-004"

        assertThrows<IllegalArgumentException> {
            consultantSearchService.searchSemantic(criteria, pageable)
        }
    }

    @Test
    fun `searchSemantic should throw exception for invalid criteria`() {
        val invalidCriteria = SemanticSearchCriteria(
            text = "", // Empty text
            topK = 0 // Invalid topK
        )
        val pageable = PageRequest.of(0, 10)

        assertThrows<IllegalArgumentException> {
            consultantSearchService.searchSemantic(invalidCriteria, pageable)
        }
    }

    @Test
    fun `getEmbeddingProviderInfo should return provider information`() {
        every { embeddingProvider.isEnabled() } returns true
        every { embeddingProvider.providerName } returns "GOOGLE_GEMINI"
        every { embeddingProvider.modelName } returns "text-embedding-004"
        every { embeddingProvider.dimension } returns 768

        val info = consultantSearchService.getEmbeddingProviderInfo()

        assertTrue(info.enabled)
        assertEquals("GOOGLE_GEMINI", info.provider)
        assertEquals("text-embedding-004", info.model)
        assertEquals(768, info.dimension)
    }
}