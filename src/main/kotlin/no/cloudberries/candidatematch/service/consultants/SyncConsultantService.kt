package no.cloudberries.candidatematch.service.consultants

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.cloudberries.candidatematch.infrastructure.adapters.toEntity
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseCvDto
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseUserDTO
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.toDomain
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.service.embedding.CvEmbeddingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Year

@Service
class SyncConsultantService(
    private val flowcaseHttpClient: FlowcaseHttpClient,
    private val consultantRepository: ConsultantRepository,
    private val cvEmbeddingService: CvEmbeddingService,
) {

    private val logger = KotlinLogging.logger {}
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    data class SyncResult(val processed: Int, val skipped: Int, val errors: List<Pair<String, String>>)

    @Scheduled(cron = "0 0 */8 * * *")
    fun scheduledSyncAll() {
        runBlocking {
            logger.info { "Running scheduled job to sync consultants and CVs" }
            syncAll()
        }
    }

    fun syncAll(batchSize: Int = 100): SyncResult {
        val users = fetchUsers().take(batchSize)
        var processed = 0
        var skipped = 0
        val errors = mutableListOf<Pair<String, String>>()
        users.forEach { u ->
            try {
                val changed = processUser(
                    u.userId,
                    u.cvId,
                    u.name
                )
                Thread.sleep(200) // to avoid hitting rate limits on Flowcase API
                // Embedding and persistence handled in processUser and CvEmbeddingService
                if (changed) processed++ else skipped++
            } catch (e: Exception) {
                logger.error(e) { "Error processing userId=${u.userId} cvId=${u.cvId}" }
                errors.add(u.userId to (e.message ?: "unknown"))
            }
        }
        logger.info { "Sync completed: processed=$processed, skipped=$skipped, errors=${errors.size}" }
        return SyncResult(
            processed,
            skipped,
            errors
        )
    }

    val file: File = File("cvs.txt")

    fun processUser(userId: String, cvId: String, name: String? = null): Boolean {
        val cvDto = fetchCvForUser(
            userId,
            cvId
        )
        val cvDomain = cvDto.toDomain()

        // Build domain Consultant and persist idempotently
        val consultant = no.cloudberries.candidatematch.domain.consultant.Consultant.builder(
            id = userId,
            defaultCvId = cvId
        )
            .withCv(cvDomain)
            .withCvAsJson(mapper.writeValueAsString(cvDomain))
            .withPersonalInfo(
                no.cloudberries.candidatematch.domain.consultant.PersonalInfo(
                    name = name ?: cvDto.name,
                    email = cvDto.email,
                    birthYear = cvDto.bornYear?.let { Year.of(it) }
                )
            )
            .withSkills(cvDomain.skillCategories.flatMap { it.skills })
            .build()

        val toSave = consultant.toEntity()
        val existing = consultantRepository.findByUserId(userId)
        val saved = if (existing == null) {
            consultantRepository.save(toSave)
        } else {
            consultantRepository.save(
                ConsultantEntity(
                    id = existing.id,
                    name = toSave.name,
                    userId = toSave.userId,
                    cvId = toSave.cvId,
                    resumeData = toSave.resumeData,
                    skills = toSave.skills
                )
            )
        }
        logger.info { "Persisted consultant userId=$userId id=${saved.id}" }
        Files.write(
            file.toPath(),
            "${mapper.writeValueAsString(saved)}\n".toByteArray(),
            StandardOpenOption.APPEND
        )

        // Delegate embedding logic (idempotent) to CvEmbeddingService
        val embedded = cvEmbeddingService.processUserCv(
            userId,
            cvId
        )
        if (embedded) logger.info { "Saved embedding for userId=$userId cvId=$cvId" } else logger.debug { "Embedding skipped for userId=$userId cvId=$cvId" }
        // Consider 'changed' if either persistence inserted/updated or embedding saved; heuristic here returns embedded flag
        return embedded || existing == null
    }

    fun fetchCvForUser(userid: String, cvId: String): FlowcaseCvDto = flowcaseHttpClient.fetchCompleteCv(userid, cvId)

    fun fetchUsers(): List<FlowcaseUserDTO> {
        val response = flowcaseHttpClient.fetchAllUsers()
        return response.flowcaseUserDTOs
    }
}
