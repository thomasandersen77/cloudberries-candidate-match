package no.cloudberries.candidatematch.service.projectrequest

import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.RequirementPriority
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.io.ByteArrayInputStream

@SpringBootTest
@ContextConfiguration(classes = [LiquibaseTestConfig::class])
@ActiveProfiles("test")
class ProjectRequestAnalysisServiceIntegrationTest @Autowired constructor(
    private val service: ProjectRequestAnalysisService,
) {
    @Test
    fun `upload, analyze and list project request with requirements`() {
        val pdfBytes = createPdfWithText(
            "Project Request\n" +
            "MUST: Kotlin developer\n" +
            "SHOULD: React experience\n"
        )
        val agg = service.analyzeAndStore(ByteArrayInputStream(pdfBytes), originalFilename = "req.pdf")

        // verify persisted aggregate
        val listed = service.listAll()
        val saved = listed.first { it.request.id == agg.request.id }
        assertEquals(agg.request.id, saved.request.id)
        // Verify that we got at least two requirements and priorities mapped
        val names = saved.requirements.map { it.name }
        assert(names.any { it.contains("Kotlin", ignoreCase = true) })
        assert(names.any { it.contains("React", ignoreCase = true) })
        val kotlinReq = saved.requirements.first { it.name.contains("Kotlin", ignoreCase = true) }
        val reactReq = saved.requirements.first { it.name.contains("React", ignoreCase = true) }
        assertEquals(RequirementPriority.MUST, kotlinReq.priority)
        assertEquals(RequirementPriority.SHOULD, reactReq.priority)
    }

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
}
