package no.cloudberries.candidatematch.controllers.scoring

import no.cloudberries.candidatematch.domain.candidate.scoring.CandidateCvScoringService
import no.cloudberries.candidatematch.service.ai.AIService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cv-score")
class CvScoreController(
    val aiService: AIService,
    val candidateCvScoringService: CandidateCvScoringService

) {

    private val logger = mu.KotlinLogging.logger {}

    @GetMapping("/{candidateId}")
    fun getCvScoreForCandidate(candidateId: String): CvScoreDto {
        logger.info { "Getting cv score for candidate $candidateId" }


        return CvScoreDto.empty(candidateId)
    }

    @GetMapping("/all")
    fun getAllCandidates(): List<CandidateDTO> {
        return candidateCvScoringService.getAllCandidates().map {
            CandidateDTO(
                it.id,
                it.name,
                it.birthYear
            )
        }
    }
}

data class CandidateDTO(
    val id: String,
    val name: String,
    val birthYear: Int,
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