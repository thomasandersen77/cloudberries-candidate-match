package no.cloudberries.candidatematch.service.notification

import mu.KotlinLogging
import no.cloudberries.candidatematch.service.ProjectRequestService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime


@Component
class NotificationScheduler(
    private val projectRequestService: ProjectRequestService,
    private val notificationService: NotificationService
) {

    private val logger = KotlinLogging.logger {  }

    /**
     * Kjører hver time for å sjekke etter forespørsler
     * med frist innen 48 timer.
     */
    @Scheduled(cron = "0 0 * * * ?") // Hver time
    fun checkDeadlinesAndNotify() {
        logger.info("Scheduler: Running job to check for upcoming deadlines...")

        val now = LocalDateTime.now()
        val limit = now.plusHours(48)

        val requestsToNotify = projectRequestService.findOpenRequestsDueWithin(now, limit)

        if (requestsToNotify.isEmpty()) {
            logger.info("Scheduler: No requests found requiring notification.")
            return
        }

        logger.info("Scheduler: Found ${requestsToNotify.size} requests to notify.")
        requestsToNotify.forEach { request ->
            notificationService.sendDeadlineReminder(request)
        }
    }
}
