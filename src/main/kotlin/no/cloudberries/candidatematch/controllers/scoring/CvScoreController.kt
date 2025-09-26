package no.cloudberries.candidatematch.controllers.scoring

import no.cloudberries.candidatematch.domain.candidate.scoring.CandidateCvScoringService
import no.cloudberries.candidatematch.service.ai.AIService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cv-score")
class CvScoreController(
    private val cvScoreAppService: no.cloudberries.candidatematch.service.scoring.CvScoreAppService,
) {

    private val logger = mu.KotlinLogging.logger {}

    @GetMapping("/{candidateId}")
    fun getCvScoreForCandidate(@PathVariable("candidateId") candidateId: String): CvScoreDto {
        logger.info { "Getting cv score for candidate $candidateId" }
        return cvScoreAppService.getScore(candidateId)
    }

    @GetMapping("/all")
    fun getAllCandidates(): List<CandidateDTO> = cvScoreAppService.listCandidates()

    @PostMapping("/{candidateId}/run")
    fun runScoreForCandidate(@PathVariable("candidateId") candidateId: String): CvScoreDto =
        cvScoreAppService.scoreCandidate(candidateId)

    @PostMapping("/run/all")
    fun runScoreForAll(): CvScoringRunResponse {
        val result = cvScoreAppService.scoreAll()
        return CvScoringRunResponse(processedCount = result.processedCount)
    }
}

data class CandidateDTO(
    val id: String,
    val name: String,
    val birthYear: Int,
)

data class CvScoringRunResponse(
    val processedCount: Int
)

data class CvScoreDto(
    val candidateId: String,
    val scorePercent: Int,
    val summary: String,
    val strengths: List<String>,
    val potentialImprovements: List<String>
) {
    companion object {
        fun empty(candidateId: String) = CvScoreDto(
            candidateId,
            0,
            "",
            emptyList(),
            emptyList()
        )
    }
}