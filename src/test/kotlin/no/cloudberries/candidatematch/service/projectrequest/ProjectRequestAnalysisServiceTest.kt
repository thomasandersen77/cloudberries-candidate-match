package no.cloudberries.candidatematch.service.projectrequest

import io.mockk.every
import io.mockk.mockk
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.ai.AIResponse
import no.cloudberries.candidatematch.infrastructure.entities.CustomerProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.entities.ProjectRequestRequirementEntity
import no.cloudberries.candidatematch.infrastructure.entities.RequirementPriority
import no.cloudberries.candidatematch.infrastructure.repositories.CustomerProjectRequestRepository
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream

class ProjectRequestAnalysisServiceTest {

    private val aiAnalysisService: AIAnalysisService = mockk(relaxed = true)
    private val repository: CustomerProjectRequestRepository = mockk(relaxed = true)

    private val service = ProjectRequestAnalysisService(
        aiAnalysisService,
        repository
    )

    @Test
    fun `analyzeAndStore parses AI JSON and persists entity`() {
        val json = """
            {
              "project_request": {
                "customer_name": "Statens Vegvesen",
                "title": "Backend Kotlin-utvikler",
                "summary": "Behov for senior backend-utvikler",
                "must_requirements": [
                  { "name": "Kotlin", "details": "5+ år, Spring Boot" }
                ],
                "should_requirements": [
                  { "name": "React", "details": "Frontend er en fordel" }
                ]
              }
            }
        """.trimIndent()

        every {
            aiAnalysisService.analyzeContent(
                any(),
                any()
            )
        } returns AIResponse(
            json,
            "gemini"
        )

        every { repository.save(any()) } answers {
            val arg0 = this.invocation.args[0] as CustomerProjectRequestEntity
            arg0.copy(
                id = 1L,
                requirements = listOf(
                    ProjectRequestRequirementEntity(
                        id = 10L,
                        name = "Kotlin",
                        details = "5+ år, Spring Boot",
                        priority = RequirementPriority.MUST
                    ),
                    ProjectRequestRequirementEntity(
                        id = 11L,
                        name = "React",
                        details = "Frontend er en fordel",
                        priority = RequirementPriority.SHOULD
                    )
                )
            )
        }

        val pdf = File("src/test/resources/politiet/cv-mal.pdf")
        FileInputStream(pdf).use { input ->
            val result = service.analyzeAndStore(
                pdfStream = input,
                originalFilename = pdf.name,
                aiProvider = AIProvider.GEMINI
            )
            assertNotNull(result.id)
            assertEquals(
                "Statens Vegvesen",
                result.customerName
            )
            assertEquals(
                2,
                result.requirements.size
            )
        }
    }
}