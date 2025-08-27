package no.cloudberries.candidatematch.service

import kotlinx.coroutines.test.runTest
import no.cloudberries.candidatematch.service.consultants.SyncConsultantService
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Ignore("Only for manual testing")
@SpringBootTest
@ActiveProfiles("integration")
@Import(LiquibaseTestConfig::class)
class FlowcaseSyncServiceTestManuel {

    @Autowired
    lateinit var syncConsultantService: SyncConsultantService

    @Test
    fun fetchUsers() {
        val users = syncConsultantService.fetchUsers()
        assertEquals(118, users.size)
    }

    @Test
    fun fetchFullCvForUsers() = runTest {
        syncConsultantService.fetchFullCvForUser()
    }
}