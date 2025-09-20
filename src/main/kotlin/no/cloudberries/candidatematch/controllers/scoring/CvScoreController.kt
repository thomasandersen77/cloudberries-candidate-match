package no.cloudberries.candidatematch.controllers.scoring

import no.cloudberries.candidatematch.domain.candidate.scoring.CandidateCvScoringService
import no.cloudberries.candidatematch.service.ai.AIService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cv-score")
class CvScoreController(
    val aiService: AIService,
    val candidateCvScoringService: CandidateCvScoringService,
    val cvScoreRepository: no.cloudberries.candidatematch.infrastructure.repositories.scoring.CvScoreRepository,
    val consultantRepository: no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository,
) {

    private val logger = mu.KotlinLogging.logger {}

    @GetMapping("/{candidateId}")
    fun getCvScoreForCandidate(@PathVariable("candidateId") candidateId: String): CvScoreDto {
        logger.info { "Getting cv score for candidate $candidateId" }
        val score = cvScoreRepository.findByCandidateUserId(candidateId)
        if (score != null) {
            val strengths = jsonArrayToList(score.strengths)
            val improvements = jsonArrayToList(score.potentialImprovements)
            return CvScoreDto(
                candidateId = candidateId,
                scorePercent = score.scorePercent,
                summary = score.summary ?: "",
                strengths = strengths,
                potentialImprovements = improvements
            )
        }
        return CvScoreDto.empty(candidateId)
    }

    @GetMapping("/all")
    fun getAllCandidates(): List<CandidateDTO> {
        // Use consultants from DB for immediate response (already synced)

        return consultantRepository.findAll().map {
            CandidateDTO(
                it.userId,
                it.name,
                it.resumeData.get("bornYear")?.asInt() ?: 0
            )
        }
    }

    private fun jsonArrayToList(node: com.fasterxml.jackson.databind.JsonNode?): List<String> =
        when {
            node == null || node.isNull -> emptyList()
            node.isArray -> node.mapNotNull { it.asText() }
            else -> emptyList()
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