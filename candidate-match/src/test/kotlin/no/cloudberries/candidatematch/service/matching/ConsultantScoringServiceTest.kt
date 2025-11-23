package no.cloudberries.candidatematch.service.matching

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.cloudberries.candidatematch.controllers.consultants.ConsultantCvDto
import no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto
import no.cloudberries.candidatematch.infrastructure.entities.scoring.CvScoreEntity
import no.cloudberries.candidatematch.infrastructure.repositories.scoring.CvScoreRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@DisplayName("ConsultantScoringService")
class ConsultantScoringServiceTest {
    
    private lateinit var cvScoreRepository: CvScoreRepository
    private lateinit var scoringService: ConsultantScoringService
    
    @BeforeEach
    fun setup() {
        cvScoreRepository = mockk()
        scoringService = ConsultantScoringService(cvScoreRepository)
    }
    
    @Nested
    @DisplayName("scoreConsultantsByCombinedRelevance")
    inner class ScoreConsultantsByCombinedRelevance {
        
        @Test
        fun `should return empty list when no consultants provided`() {
            val result = scoringService.scoreConsultantsByCombinedRelevance(
                consultants = emptyList(),
                requiredSkills = listOf("Kotlin", "Spring"),
                minCandidates = 5,
                maxCandidates = 15
            )
            
            assertTrue(result.isEmpty())
        }
        
        @Test
        fun `should select consultants based on skill match when CV scores available`() {
            // Arrange
            val consultants = listOf(
                createConsultant(1L, "user1", "Alice", skills = listOf("Kotlin", "Spring")),
                createConsultant(2L, "user2", "Bob", skills = listOf("Kotlin", "Java")),
                createConsultant(3L, "user3", "Charlie", skills = listOf("Python", "Django"))
            )
            
            val cvScores = listOf(
                createCvScore("user1", 80), // High CV score
                createCvScore("user2", 60), // Medium CV score
                createCvScore("user3", 90)  // High CV score but poor skill match
            )
            
            every { cvScoreRepository.findByCandidateUserIdIn(any()) } returns cvScores
            
            // Act
            val result = scoringService.scoreConsultantsByCombinedRelevance(
                consultants = consultants,
                requiredSkills = listOf("Kotlin", "Spring"),
                minCandidates = 2,
                maxCandidates = 3
            )
            
            // Assert
            assertEquals(3, result.size)
            // Alice should be first (100% skill match + 80% CV = high combined)
            assertEquals("Alice", result[0].name)
            // Bob should be second (50% skill match + 60% CV = medium combined)
            assertEquals("Bob", result[1].name)
            // Charlie should be last (0% skill match + 90% CV = lower combined)
            assertEquals("Charlie", result[2].name)
            
            verify { cvScoreRepository.findByCandidateUserIdIn(listOf("user1", "user2", "user3")) }
        }
        
        @Test
        fun `should use default CV score when no CV score exists for consultant`() {
            // Arrange
            val consultants = listOf(
                createConsultant(1L, "user1", "Alice", skills = listOf("Kotlin")),
                createConsultant(2L, "user2", "Bob", skills = listOf("Java"))
            )
            
            // Only one consultant has CV score
            every { cvScoreRepository.findByCandidateUserIdIn(any()) } returns listOf(
                createCvScore("user1", 80)
            )
            
            // Act
            val result = scoringService.scoreConsultantsByCombinedRelevance(
                consultants = consultants,
                requiredSkills = listOf("Kotlin"),
                minCandidates = 2,
                maxCandidates = 2
            )
            
            // Assert
            assertEquals(2, result.size)
            // Alice should be first (skill match + actual CV score)
            assertEquals("Alice", result[0].name)
        }
        
        @Test
        fun `should guarantee minimum candidates even with low scores`() {
            // Arrange
            val consultants = listOf(
                createConsultant(1L, "user1", "Alice", skills = listOf("Python")),
                createConsultant(2L, "user2", "Bob", skills = listOf("Ruby")),
                createConsultant(3L, "user3", "Charlie", skills = listOf("Go")),
                createConsultant(4L, "user4", "David", skills = listOf("Rust")),
                createConsultant(5L, "user5", "Eve", skills = listOf("C++"))
            )
            
            every { cvScoreRepository.findByCandidateUserIdIn(any()) } returns emptyList()
            
            // Act - Request consultants for skills none of them have
            val result = scoringService.scoreConsultantsByCombinedRelevance(
                consultants = consultants,
                requiredSkills = listOf("Kotlin", "Spring"), // None match
                minCandidates = 5,
                maxCandidates = 10
            )
            
            // Assert - Should still return 5 consultants despite low scores
            assertEquals(5, result.size)
        }
        
        @Test
        fun `should respect maxCandidates limit`() {
            // Arrange
            val consultants = (1..20).map { i ->
                createConsultant(i.toLong(), "user$i", "Person$i", skills = listOf("Kotlin"))
            }
            
            every { cvScoreRepository.findByCandidateUserIdIn(any()) } returns emptyList()
            
            // Act
            val result = scoringService.scoreConsultantsByCombinedRelevance(
                consultants = consultants,
                requiredSkills = listOf("Kotlin"),
                minCandidates = 5,
                maxCandidates = 10
            )
            
            // Assert
            assertEquals(10, result.size)
        }
        
        @Test
        fun `should handle empty required skills gracefully`() {
            // Arrange
            val consultants = listOf(
                createConsultant(1L, "user1", "Alice", skills = listOf("Kotlin")),
                createConsultant(2L, "user2", "Bob", skills = listOf("Java"))
            )
            
            every { cvScoreRepository.findByCandidateUserIdIn(any()) } returns listOf(
                createCvScore("user1", 80),
                createCvScore("user2", 60)
            )
            
            // Act - No skills specified
            val result = scoringService.scoreConsultantsByCombinedRelevance(
                consultants = consultants,
                requiredSkills = emptyList(),
                minCandidates = 2,
                maxCandidates = 2
            )
            
            // Assert - Should prioritize by CV score when no skills specified
            assertEquals(2, result.size)
            assertEquals("Alice", result[0].name) // Higher CV score
        }
        
        @Test
        fun `should handle consultants with no skills`() {
            // Arrange
            val consultants = listOf(
                createConsultant(1L, "user1", "Alice", skills = emptyList()),
                createConsultant(2L, "user2", "Bob", skills = listOf("Kotlin"))
            )
            
            every { cvScoreRepository.findByCandidateUserIdIn(any()) } returns listOf(
                createCvScore("user1", 90),
                createCvScore("user2", 50)
            )
            
            // Act
            val result = scoringService.scoreConsultantsByCombinedRelevance(
                consultants = consultants,
                requiredSkills = listOf("Kotlin"),
                minCandidates = 2,
                maxCandidates = 2
            )
            
            // Assert
            assertEquals(2, result.size)
            // Bob should be first despite lower CV score (has skill match)
            assertEquals("Bob", result[0].name)
        }
        
        @Test
        fun `should perform case-insensitive skill matching`() {
            // Arrange
            val consultants = listOf(
                createConsultant(1L, "user1", "Alice", skills = listOf("kotlin", "spring")),
                createConsultant(2L, "user2", "Bob", skills = listOf("KOTLIN", "JAVA"))
            )
            
            every { cvScoreRepository.findByCandidateUserIdIn(any()) } returns listOf(
                createCvScore("user1", 70),
                createCvScore("user2", 70)
            )
            
            // Act - Mixed case required skills
            val result = scoringService.scoreConsultantsByCombinedRelevance(
                consultants = consultants,
                requiredSkills = listOf("Kotlin", "Spring"),
                minCandidates = 2,
                maxCandidates = 2
            )
            
            // Assert
            assertEquals(2, result.size)
            // Alice should be first (matches both skills)
            assertEquals("Alice", result[0].name)
        }
        
        @Test
        fun `should handle fewer consultants than minCandidates`() {
            // Arrange
            val consultants = listOf(
                createConsultant(1L, "user1", "Alice", skills = listOf("Kotlin")),
                createConsultant(2L, "user2", "Bob", skills = listOf("Java"))
            )
            
            every { cvScoreRepository.findByCandidateUserIdIn(any()) } returns emptyList()
            
            // Act - Request more than available
            val result = scoringService.scoreConsultantsByCombinedRelevance(
                consultants = consultants,
                requiredSkills = listOf("Kotlin"),
                minCandidates = 5,
                maxCandidates = 10
            )
            
            // Assert - Should return all available consultants
            assertEquals(2, result.size)
        }
        
        @Test
        fun `should correctly weight skill match and CV quality`() {
            // Arrange
            val consultants = listOf(
                createConsultant(1L, "user1", "Alice", 
                    skills = listOf("Kotlin", "Spring")), // 100% skill match
                createConsultant(2L, "user2", "Bob", 
                    skills = emptyList()) // 0% skill match
            )
            
            // Bob has perfect CV, Alice has average CV
            every { cvScoreRepository.findByCandidateUserIdIn(any()) } returns listOf(
                createCvScore("user1", 50),  // 0.5 normalized
                createCvScore("user2", 100)  // 1.0 normalized
            )
            
            // Act
            val result = scoringService.scoreConsultantsByCombinedRelevance(
                consultants = consultants,
                requiredSkills = listOf("Kotlin", "Spring"),
                minCandidates = 2,
                maxCandidates = 2
            )
            
            // Assert
            // Alice: (0.7 * 1.0) + (0.3 * 0.5) = 0.85
            // Bob: (0.7 * 0.0) + (0.3 * 1.0) = 0.30
            assertEquals("Alice", result[0].name) // Higher combined despite lower CV
        }
    }
    
    // Helper methods
    
    private fun createConsultant(
        id: Long,
        userId: String,
        name: String,
        cvId: String = "cv-$userId",
        skills: List<String> = emptyList()
    ): ConsultantWithCvDto {
        return ConsultantWithCvDto(
            id = id,
            userId = userId,
            name = name,
            cvId = cvId,
            skills = skills,
            cvs = listOf(
                ConsultantCvDto(
                    id = 1L,
                    versionTag = "v1",
                    qualityScore = 80,
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
    }
    
    private fun createCvScore(userId: String, scorePercent: Int): CvScoreEntity {
        return CvScoreEntity(
            id = null,
            candidateUserId = userId,
            name = "Name for $userId",
            scorePercent = scorePercent,
            summary = "Test summary",
            strengths = null,
            potentialImprovements = null
        )
    }
}
