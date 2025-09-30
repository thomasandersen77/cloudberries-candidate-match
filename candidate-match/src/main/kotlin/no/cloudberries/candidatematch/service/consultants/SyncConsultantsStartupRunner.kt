package no.cloudberries.candidatematch.service.consultants

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

//@Component
@Order(50)
@Profile("!test")
class SyncConsultantsStartupRunner(
    private val syncService: SyncConsultantService,
    @param:Value($$"${sync.consultants.on-startup:true}") private val enabled: Boolean,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        if (enabled) {
            try {
                syncService.syncAll()
            } catch (e: Exception) {
                // Do not fail startup on sync issues
            }
        }
    }
}