package no.cloudberries.candidatematch.controllers.consultants

import mu.KotlinLogging
import no.cloudberries.candidatematch.service.consultants.SyncConsultantService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/consultants/sync")
class ConsultantSyncController(
    private val syncService: SyncConsultantService,
) {
    private val logger = KotlinLogging.logger { }

    data class SyncResponse(val total: Int, val succeeded: Int, val failed: Int)

    @PostMapping("/run")
    @Timed
    fun syncAll(): SyncResponse {
        logger.info { "POST /api/consultants/sync/run" }
        val res = syncService.syncAll()
        return SyncResponse(
            res.total,
            res.succeeded,
            res.failed
        )
    }

    @PostMapping("/{userId}/{cvId}")
    @Timed
    fun syncOne(@PathVariable userId: String, @PathVariable cvId: String): SyncResponse {
        logger.info { "POST /api/consultants/sync/$userId/$cvId" }
        val res = syncService.syncOne(userId)
        return SyncResponse(
            res.total,
            res.succeeded,
            res.failed
        )
    }
}
