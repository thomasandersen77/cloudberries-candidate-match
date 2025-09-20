package no.cloudberries.candidatematch.service

import LiquibaseTestConfig
import com.github.tomakehurst.wiremock.client.WireMock.*
import kotlinx.coroutines.test.runTest
import no.cloudberries.candidatematch.service.consultants.SyncConsultantService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(LiquibaseTestConfig::class)
@AutoConfigureWireMock(port = 0) // Bruker en tilfeldig ledig port
class FlowcaseSyncServiceIntegrationTest {

    @Autowired
    private lateinit var syncConsultantService: SyncConsultantService

    @Test
    fun `fetchCvForUser skal hente og parse en delvis CV`() {
        // Gitt (Arrange)
        val userId = "user123"
        val cvId = "cv456"

        // Når (Act)
        val cv = syncConsultantService.fetchCvForUser(userId, cvId)

        // Da (Assert)
        assertNotNull(cv)
    }

    @Test
    fun `fetchCvForUser skal hente og parse en komplett CV`() {
        // Gitt (Arrange)
        val userId = "thomas"
        val cvId = "andersen"

        // Når (Act)
        val cv = syncConsultantService.fetchCvForUser(userId, cvId)

        // Da (Assert)
        assertNotNull(cv)
    }

    @Test
    fun `fetchUsers skal hente og returnere en liste av brukere`() {
        // Gitt (Arrange) - WireMock stubs er allerede lastet

        // Når (Act)
        val users = syncConsultantService.fetchUsers()

        // Da (Assert)
        assertNotNull(users)
        assertEquals(1, users.size)
        val user = users.first()
        assertEquals("user123", user.userId)
        assertEquals("Ola Nordmann", user.name)
    }

    @Test
    fun `fetchFullCvForUser skal iterere gjennom brukere og hente CV for hver`() = runTest {
        // Denne testen verifiserer at orkestreringsmetoden kaller på de underliggende metodene som forventet.
        // Gitt (Arrange) - WireMock stubs er allerede lastet

        // Når (Act)
        // Vi kaller hovedmetoden. Siden den har @Scheduled, kaller vi den direkte i testen.
        syncConsultantService.scheduledSyncAll()

        // Da (Assert)
        // Verifiser at kallene til WireMock ble gjort som forventet.
        verify(getRequestedFor(urlPathEqualTo("/v2/users/search")))
        verify(getRequestedFor(urlPathEqualTo("/v3/cvs/user123/cv456")))
    }
}