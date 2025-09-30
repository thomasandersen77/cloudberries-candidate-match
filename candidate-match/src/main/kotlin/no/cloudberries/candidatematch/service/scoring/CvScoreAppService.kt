package no.cloudberries.candidatematch.service.scoring

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import mu.KotlinLogging
import no.cloudberries.candidatematch.controllers.scoring.CandidateDTO
import no.cloudberries.candidatematch.controllers.scoring.CvScoreDto
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.scoring.CVEvaluation
import no.cloudberries.candidatematch.infrastructure.entities.scoring.CvScoreEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.scoring.CvScoreRepository
import org.springframework.stereotype.Service

@Service
class CvScoreAppService(
    private val consultantRepository: ConsultantRepository,
    private val cvScoreRepository: CvScoreRepository,
    private val scoreCandidateService: ScoreCandidateService,
) {
    private val logger = KotlinLogging.logger { }
    private val mapper: ObjectMapper = jacksonObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    fun getScore(candidateId: String): CvScoreDto {
        val entity = cvScoreRepository.findByCandidateUserId(candidateId)
            ?: return CvScoreDto.empty(candidateId)
        return entityToDto(candidateId, entity)
    }

    fun listCandidates(): List<CandidateDTO> = consultantRepository.findAll().map {
        CandidateDTO(
            id = it.userId,
            name = it.name,
            birthYear = it.resumeData.get("bornYear")?.asInt() ?: 0
        )
    }

    fun scoreCandidate(candidateId: String, aiProvider: AIProvider = AIProvider.GEMINI): CvScoreDto {
        val consultant = consultantRepository.findByUserId(candidateId)
            ?: throw IllegalArgumentException("Consultant with userId=$candidateId not found")
logger.info { "Scoring CV for userId=$candidateId" }

val cv = consultant.resumeData.toString()
val evaluation: CVEvaluation = scoreCandidateService.performCvScoring(
    aiProvider = aiProvider,
    cv = cv,
    consultantName = consultant.name
)
logger.info { "Finished scoring CV for userId=$candidateId with score: ${evaluation.scorePercentage}" }
        val strengthsNode = mapper.readTree(mapper.writeValueAsString(evaluation.strengths ?: emptyList<String>()))
        val improvementsNode = mapper.readTree(mapper.writeValueAsString(evaluation.improvements ?: emptyList<String>()))

        val existing = cvScoreRepository.findByCandidateUserId(candidateId)
        val saved = cvScoreRepository.save(
            CvScoreEntity(
                id = existing?.id,
                candidateUserId = consultant.userId,
                name = consultant.name,
                scorePercent = evaluation.scorePercentage,
                summary = evaluation.summary ?: "",
                strengths = strengthsNode,
                potentialImprovements = improvementsNode
            )
        )
        logger.info { "Stored CV score for ${consultant.name} (${saved.id})" }
        return entityToDto(candidateId, saved)
    }

    data class ScoreAllResult(val processedCount: Int)

    fun scoreAll(aiProvider: AIProvider = AIProvider.GEMINI): ScoreAllResult {
        val consultants = consultantRepository.findAll()
        var processed = 0
        consultants.forEach { entity ->
            try {
                val dto = scoreCandidate(entity.userId, aiProvider)
                if (dto.scorePercent > 0) processed++
            } catch (e: Exception) {
                logger.error(e) { "Failed to score consultant ${entity.name}" }
            }
        }
        return ScoreAllResult(processed)
    }

    private fun entityToDto(candidateId: String, e: CvScoreEntity): CvScoreDto {
        val strengths = jsonArrayToList(e.strengths)
        val improvements = jsonArrayToList(e.potentialImprovements)
        return CvScoreDto(
            candidateId = candidateId,
            scorePercent = e.scorePercent,
            summary = e.summary ?: "",
            strengths = strengths,
            potentialImprovements = improvements
        )
    }

    private fun jsonArrayToList(node: com.fasterxml.jackson.databind.JsonNode?): List<String> = when {
        node == null || node.isNull -> emptyList()
        node.isArray -> node.mapNotNull { it.asText() }
        else -> emptyList()
    }
}