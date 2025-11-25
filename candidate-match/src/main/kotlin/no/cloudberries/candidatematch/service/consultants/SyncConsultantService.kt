package no.cloudberries.candidatematch.service.consultants

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.consultant.ConsultantAdapter
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseCvDto
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseUserDTO
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SyncConsultantService(
    private val flowcaseHttpClient: FlowcaseHttpClient,
    private val consultantAdapter: ConsultantAdapter,
    private val persistenceService: ConsultantPersistenceService,
) {

    private val logger = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 */8 * * *")
    fun fetchFullCvForUser() {
        runBlocking {
            try {
                logger.info { "Running scheduled job to fetch full CV for all users" }
                flowcaseHttpClient.fetchAllUsers().flowcaseUserDTOs.forEach {
                    delay(500) // Increased delay to 500 ms
                    fetchCvForUser(
                        it.userId,
                        it.cvId
                    )
                }
            } catch (e: IllegalArgumentException) {
                if (e.message?.contains("Expected URL scheme") == true) {
                    logger.warn { "Flowcase URL not configured, skipping scheduled sync. Set FLOWCASE_BASE_URL environment variable." }
                } else {
                    throw e
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to run scheduled CV sync: ${e.message}" }
            }
        }
    }

    fun fetchCvForUser(userid: String, cvId: String): FlowcaseCvDto = flowcaseHttpClient.fetchCompleteCv(
        userid,
        cvId
    )

    fun fetchUsers(): List<FlowcaseUserDTO> = flowcaseHttpClient
        .fetchAllUsers()
        .flowcaseUserDTOs


    @Transactional
    fun syncAll(): SyncResult {
        val users = fetchUsers()
        var success = 0
        var failures = 0
        var created = 0
        var updated = 0
        val batchSize = 10 // Process in smaller batches

        logger.info { "Starting sync for ${users.size} consultants in batches of $batchSize" }

        users.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            logger.info { "Processing batch ${batchIndex + 1}/${(users.size + batchSize - 1) / batchSize}" }

            batch.forEach { u ->
                try {
                    val consultant = consultantAdapter.fetchConsultant(u.userId)
                    val result = persistenceService.upsertConsultantWithCv(consultant)

                    when (result.operation) {
                        UpsertOperation.CREATED -> {
                            created++
                            logger.info { "Created new consultant ${u.userId}" }
                        }

                        UpsertOperation.UPDATED -> {
                            updated++
                            logger.info { "Updated existing consultant ${u.userId}" }
                        }
                    }
                    success++
                } catch (e: Exception) {
                    when {
                        e.message?.contains("429") == true -> {
                            logger.warn { "Rate limit hit for consultant ${u.userId}, will retry later" }
                            // Could implement retry logic here
                        }

                        else -> {
                            logger.error(e) { "Failed syncing consultant ${u.userId}: ${e.message}" }
                        }
                    }
                    failures++
                }

                // Add delay between each consultant to avoid rate limiting
                Thread.sleep(200) // 0,2-second delays between API calls
            }

            // Add longer delay between batches
            if (batchIndex < users.chunked(batchSize).size - 1) {
                logger.info { "Batch ${batchIndex + 1} complete, waiting before next batch..." }
                Thread.sleep(500) // 500 ms delay between batches
            }
        }

        logger.info { "Sync complete. Success: $success (Created: $created, Updated: $updated), Failures: $failures" }
        return SyncResult(
            total = users.size,
            succeeded = success,
            failed = failures,
            created = created,
            updated = updated
        )
    }

    // Improved: persist a single consultant + CV with upsert logic
    @Transactional
    fun syncOne(userId: String): SyncResult {
        return try {
            val consultant = consultantAdapter.fetchConsultant(userId)
            val result = persistenceService.upsertConsultantWithCv(consultant)

            val (created, updated) = when (result.operation) {
                UpsertOperation.CREATED -> {
                    logger.info { "Created new consultant $userId" }
                    Pair(
                        1,
                        0
                    )
                }

                UpsertOperation.UPDATED -> {
                    logger.info { "Updated existing consultant $userId" }
                    Pair(
                        0,
                        1
                    )
                }
            }

            SyncResult(
                total = 1,
                succeeded = 1,
                failed = 0,
                created = created,
                updated = updated
            )
        } catch (e: Exception) {
            when {
                e.message?.contains("429") == true -> {
                    logger.warn { "Rate limit hit for consultant $userId" }
                }

                else -> {
                    logger.error(e) { "Failed syncing consultant $userId: ${e.message}" }
                }
            }
            SyncResult(
                total = 1,
                succeeded = 0,
                failed = 1,
                created = 0,
                updated = 0
            )
        }
    }

    data class SyncResult(
        val total: Int,
        val succeeded: Int,
        val failed: Int,
        val created: Int = 0,
        val updated: Int = 0
    )
}

enum class UpsertOperation {
    CREATED, UPDATED
}
