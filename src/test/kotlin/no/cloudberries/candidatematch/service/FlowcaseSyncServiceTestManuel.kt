package no.cloudberries.candidatematch.service

import org.junit.Ignore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Ignore("Only for manual testing")
@SpringBootTest
@ActiveProfiles("test")
@Import(LiquibaseTestConfig::class)
class FlowcaseSyncServiceTestManuel {

    @Autowired
    lateinit var flowcaseSyncService: FlowcaseSyncService

    @Test
    fun fetchUsers() {
        val users = flowcaseSyncService.fetchUsers()
        assertEquals(119, users.size)
    }

    @Test
    fun fetchFullCvForUsers() {
        flowcaseSyncService.fetchFullCvForUser()
    }
}