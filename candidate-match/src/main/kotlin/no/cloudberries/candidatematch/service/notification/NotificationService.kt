package no.cloudberries.candidatematch.service.notification

import no.cloudberries.candidatematch.domain.ProjectRequest

/**
 * Et grensesnitt for å sende varsler.
 * Dette lar oss bytte ut implementasjonen (f.eks. fra logging til e-post)
 * uten å endre koden som bruker tjenesten.
 */
interface NotificationService {
    fun sendDeadlineReminder(projectRequest: ProjectRequest)
}