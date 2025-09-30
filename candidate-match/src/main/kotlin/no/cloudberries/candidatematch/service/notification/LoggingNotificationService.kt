package no.cloudberries.candidatematch.service.notification

import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.ProjectRequest
import org.springframework.stereotype.Service


@Service
class LoggingNotificationService : NotificationService {

    private val logger = KotlinLogging.logger {  }


    override fun sendDeadlineReminder(projectRequest: ProjectRequest) {
        val emailContent = """
            --------------------------------------------------
            REMINDER: Deadline Approaching!
            --------------------------------------------------
            To: ${projectRequest.responsibleSalespersonEmail}
            Customer: ${projectRequest.customerName}
            Deadline: ${projectRequest.responseDeadline}
            Request: ${projectRequest.requestDescription}
            --------------------------------------------------
        """.trimIndent()

        logger.info("Sending notification:\n{}", emailContent)
        // I en senere feature vil e-post-logikken vært her.
    }
}