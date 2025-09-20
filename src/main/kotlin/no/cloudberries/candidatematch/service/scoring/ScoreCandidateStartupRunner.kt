package no.cloudberries.candidatematch.service.scoring

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.scoring.CVEvaluation
import no.cloudberries.candidatematch.health.HealthService
import no.cloudberries.candidatematch.infrastructure.entities.scoring.CvScoreEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.scoring.CvScoreRepository
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(100)
class ScoreCandidateStartupRunner(
    private val consultantRepository: ConsultantRepository,
    private val scoreCandidateService: ScoreCandidateService,
    private val cvScoreRepository: CvScoreRepository,
    private val healthService: HealthService
) {

    private val logger = KotlinLogging.logger {}
    private val mapper: ObjectMapper = jacksonObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    fun run() {
        // Only run if AI is configured; otherwise skip silently
        if (!healthService.areAIConfigured()) {
            logger.warn { "Skipping CV scoring on startup: AI not configured." }
            return
        }

        val consultants = consultantRepository.findAll()
        logger.info { "Starting CV scoring for ${consultants.size} consultants..." }

        consultants.forEach { entity ->
            try {
                // skip if already scored
                val existing = cvScoreRepository.findByCandidateUserId(entity.userId)
                if (existing != null) {
                    return@forEach
                }

                val evaluation: CVEvaluation = scoreCandidateService.performCvScoring(
                    cv = entity.resumeData.toString(),
                    consultantName = entity.name
                )

                val strengthsNode =
                    mapper.readTree(mapper.writeValueAsString(evaluation.strengths ?: emptyList<String>()))
                val improvementsNode =
                    mapper.readTree(mapper.writeValueAsString(evaluation.improvements ?: emptyList<String>()))

                val saved = cvScoreRepository.save(
                    CvScoreEntity(
                        candidateUserId = entity.userId,
                        name = entity.name,
                        scorePercent = evaluation.scorePercentage,
                        summary = evaluation.summary ?: "",
                        strengths = strengthsNode,
                        potentialImprovements = improvementsNode
                    )
                )
                logger.info { "Stored CV score for ${entity.name} (${saved.id})" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to score consultant ${entity.name}" }
            }
        }

        logger.info { "CV scoring on startup complete." }
    }
}