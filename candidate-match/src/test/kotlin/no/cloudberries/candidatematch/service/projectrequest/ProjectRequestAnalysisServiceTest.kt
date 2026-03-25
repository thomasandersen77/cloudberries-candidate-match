package no.cloudberries.candidatematch.service.projectrequest

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.cloudberries.ai.port.AnalyzedProjectRequest
import no.cloudberries.ai.port.ProjectRequestAnalysisPort
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.CustomerProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.ProjectRequestRequirementEntity
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.RequirementPriority
import no.cloudberries.candidatematch.infrastructure.repositories.projectrequest.CustomerProjectRequestRepository
import no.cloudberries.candidatematch.infrastructure.repositories.projectrequest.ProjectRequestRequirementRepository
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

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
        val port = mockk<ProjectRequestAnalysisPort>()
        val service = ProjectRequestAnalysisService(customerRepo, reqRepo, port)

        val pdfBytes = createPdfWithText(
            "Project Req\nMUST: Kotlin\nSHOULD: React"
        )

        // Mocks: repository saves
        every { customerRepo.save(any()) } answers { firstArg<CustomerProjectRequestEntity>().copy(id = 42L) }
        every { reqRepo.saveAll(any<Iterable<ProjectRequestRequirementEntity>>()) } answers { firstArg<Iterable<ProjectRequestRequirementEntity>>().toList() }

        // AI returns analyzed project request
        every { port.analyzeProjectRequest(any(), any()) } returns AnalyzedProjectRequest(
            customerName = "Test Customer",
            summary = "AI SUMMARY",
            mustRequirements = listOf("Kotlin developer"),
            shouldRequirements = listOf("React experience"),
            uploadedAt = LocalDateTime.now(),
            deadlineDate = null
        )

        // Act
        val agg = service.analyzeAndStore(ByteArrayInputStream(pdfBytes), originalFilename = "req.pdf")

        // Assert
        assertEquals(42L, agg.request.id)
        assertEquals("AI SUMMARY", agg.request.summary)
        assertEquals(2, agg.requirements.size)
        verify(exactly = 1) { port.analyzeProjectRequest(any(), any()) }
    }
}
