package no.cloudberries.candidatematch.service.projectrequest

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.cloudberries.candidatematch.config.ProjectRequestAnalysisConfig
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.CustomerProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.ProjectRequestRequirementEntity
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.RequirementPriority
import no.cloudberries.candidatematch.infrastructure.repositories.projectrequest.CustomerProjectRequestRepository
import no.cloudberries.candidatematch.infrastructure.repositories.projectrequest.ProjectRequestRequirementRepository
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import no.cloudberries.candidatematch.service.projectrequest.parser.RequirementParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class ProjectRequestAnalysisServiceTest {

    private fun createPdfWithText(text: String): ByteArray {
        PDDocument().use { doc ->
            val page = PDPage()
            doc.addPage(page)
            PDPageContentStream(doc, page).use { content ->
                content.beginText()
                content.setFont(PDType1Font.HELVETICA, 12f)
                content.newLineAtOffset(50f, 750f)
                text.lines().forEachIndexed { idx, line ->
                    if (idx > 0) content.newLineAtOffset(0f, -18f)
                    content.showText(line)
                }
                content.endText()
            }
            val baos = java.io.ByteArrayOutputStream()
            doc.save(baos)
            return baos.toByteArray()
        }
    }

    @Test
    fun `analyzeAndStore should use AI summary when enabled`() {
        // Arrange
        val customerRepo = mockk<CustomerProjectRequestRepository>()
        val reqRepo = mockk<ProjectRequestRequirementRepository>()
        val parser = mockk<RequirementParser>()
        val ai = mockk<AIAnalysisService>()
        val cfg = ProjectRequestAnalysisConfig(aiEnabled = true, provider = AIProvider.GEMINI)

        val aiResponseParser = mockk<AIResponseParser>()
        val service = ProjectRequestAnalysisService(customerRepo, reqRepo, parser, ai, cfg, aiResponseParser)

        val pdfBytes = createPdfWithText(
            "Project Req\nMUST: Kotlin\nSHOULD: React"
        )

        // Mocks: repository saves
        every { customerRepo.save(any()) } answers { firstArg<CustomerProjectRequestEntity>().copy(id = 42L) }
        every { reqRepo.saveAll(any<Iterable<ProjectRequestRequirementEntity>>()) } answers { firstArg<Iterable<ProjectRequestRequirementEntity>>().toList() }

        // Parser returns structured requirements
        every { parser.parse(any()) } answers {
            listOf(
                no.cloudberries.candidatematch.service.projectrequest.parser.ParsedRequirement(
                    name = "Kotlin developer", details = null, priority = RequirementPriority.MUST
                ),
                no.cloudberries.candidatematch.service.projectrequest.parser.ParsedRequirement(
                    name = "React experience", details = null, priority = RequirementPriority.SHOULD
                ),
            )
        }

        // AI returns summary content
        every { ai.analyzeContent(any(), AIProvider.GEMINI) } returns no.cloudberries.candidatematch.domain.ai.AIResponse(
            content = "AI SUMMARY", modelUsed = "GEMINI"
        )
        
        // AI response parser returns processed response
        every { aiResponseParser.parseAIResponse(any(), any(), any()) } returns ProcessedAIResponse(
            customerName = "Test Customer",
            summary = "AI SUMMARY",
            mustRequirements = listOf("Kotlin developer"),
            shouldRequirements = listOf("React experience"),
            uploadedAt = java.time.LocalDateTime.now(),
            deadlineDate = null
        )

        // Act
        val agg = service.analyzeAndStore(ByteArrayInputStream(pdfBytes), originalFilename = "req.pdf")

        // Assert
        assertEquals(42L, agg.request.id)
        assertEquals("AI SUMMARY", agg.request.summary)
        assertEquals(2, agg.requirements.size)
        verify(exactly = 1) { ai.analyzeContent(any(), AIProvider.GEMINI) }
    }
}
