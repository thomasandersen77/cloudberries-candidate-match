package no.cloudberries.candidatematch.controllers.matching
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.service.ai.AIService
import no.cloudberries.candidatematch.utils.PdfUtils
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream

// Define a simple request data class
data class MatchApiRequest(val projectRequestText: String)

@RestController
@RequestMapping("/api/matches")
class MatchingController(
    private val aIService: AIService // Or your primary AIService implementation
    // You will also inject services here to get consultant CVs
) {

    private val logger = KotlinLogging.logger { }

    @PostMapping
    fun findMatches(@RequestBody request: MatchApiRequest): List<CandidateMatchResponse> {
        logger.info("Received match request for: ${request.projectRequestText.take(150)}...")

        // Example using a local PDF (kept for reference). Real use should use the upload endpoint.
        val cvText = PdfUtils.extractText(FileInputStream(File("src/main/resources/Thomas-Andersen_CV.pdf")))
        val consultantName = "Thomas Andersen"

        val matchResponse = aIService.matchCandidate(
            cv = cvText,
            request = request.projectRequestText,
            consultantName = consultantName,
            aiProvider = AIProvider.GEMINI
        )
        return listOf(matchResponse)
    }

    // New: upload a PDF and send its text to the AI, following the existing logic
    @PostMapping(
        path = ["/upload"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun findMatchesFromPdf(
        @RequestPart("file") file: MultipartFile,
        @RequestPart("projectRequestText") projectRequestText: String,
    ): List<CandidateMatchResponse> {
        logger.info("Received match request with uploaded PDF: ${file.originalFilename}")
        val cvText = PdfUtils.extractText(file.inputStream)
        val consultantName = file.originalFilename?.substringBeforeLast('.') ?: "Uploaded CV"
        val matchResponse = aIService.matchCandidate(
            aiProvider = AIProvider.GEMINI,
            cv = cvText,
            request = projectRequestText,
            consultantName = consultantName,
        )
        return listOf(matchResponse)
    }
}
