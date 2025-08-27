package no.cloudberries.candidatematch.controllers.matching
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.service.ai.AIService
import no.cloudberries.candidatematch.utils.PdfUtils
import org.springframework.web.bind.annotation.*
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

        // --- THIS IS WHERE YOUR CORE LOGIC GOES ---
        // 1. Get all relevant consultants from your database or Flowcase.
        // 2. For each consultant, call your AIService to get a match score.
        //    (For simplicity, this example just matches one hardcoded consultant)

        val cvText = PdfUtils.extractText(FileInputStream(File("src/main/resources/Thomas-Andersen_CV.pdf")))
        val consultantName = "Thomas Andersen"

        val matchResponse = aIService.matchCandidate(
            cv = cvText,
            request = request.projectRequestText,
            consultantName = consultantName,
            aiProvider = AIProvider.GEMINI
        )

        // 3. Collect the responses, sort by score, and return the top N.
        // This is a simplified list of one for the example.
        return listOf(matchResponse)
    }
}