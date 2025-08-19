package no.cloudberries.candidatematch.service

import LiquibaseTestConfig
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.cloudberries.candidatematch.integration.flowcase.MultiLangText
import no.cloudberries.candidatematch.integration.flowcase.TechnologySkillDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    private lateinit var flowcaseSyncService: FlowcaseSyncService

    @Test
    fun `fetchCvForUser skal hente og parse en delvis CV`() {
        // Gitt (Arrange)
        val userId = "user123"
        val cvId = "cv456"

        // Når (Act)
        val cvDto = flowcaseSyncService.fetchCvForUser(userId, cvId)

        // Da (Assert)
        assertNotNull(cvDto)
        assertEquals("Ola Nordmann", cvDto.name)
        assertEquals("Senior Utvikler", cvDto.title?.text)
        assertEquals(1, cvDto.projectExperiences.size)
        assertEquals(2, cvDto.projectExperiences.first().projectExperienceSkills.size)
        assertEquals("Java", cvDto.projectExperiences.first().projectExperienceSkills.first().tags?.text)
    }

    @Test
    fun `fetchCvForUser skal hente og parse en komplett CV`() {
        // Gitt (Arrange)
        val userId = "thomas"
        val cvId = "andersen"

        // Når (Act)
        val cvDto = flowcaseSyncService.fetchCvForUser(userId, cvId)

        // Da (Assert)
        assertNotNull(cvDto)
        assertEquals("thomas andersen",cvDto.name.lowercase())
        assertEquals("Senior konsulent", cvDto.title?.text)
        assertEquals(17, cvDto.projectExperiences.size)
        assertEquals(0, cvDto.projectExperiences.first().projectExperienceSkills.size)
            assertTrue {  cvDto.projectExperiences.flatMap { it.projectExperienceSkills }.contains(TechnologySkillDto(MultiLangText("Java", "Java"), 0)) }
    }

    @Test
    fun `fetchUsers skal hente og returnere en liste av brukere`() {
        // Gitt (Arrange) - WireMock stubs er allerede lastet

        // Når (Act)
        val users = flowcaseSyncService.fetchUsers()

        // Da (Assert)
        assertNotNull(users)
        assertEquals(1, users.size)
        val user = users.first()
        assertEquals("user123", user.userId)
        assertEquals("Ola Nordmann", user.name)
    }

    @Test
    fun `fetchFullCvForUser skal iterere gjennom brukere og hente CV for hver`() {
        // Denne testen verifiserer at orkestreringsmetoden kaller på de underliggende metodene som forventet.
        // Gitt (Arrange) - WireMock stubs er allerede lastet

        // Når (Act)
        // Vi kaller hovedmetoden. Siden den har @Scheduled, kaller vi den direkte i testen.
        flowcaseSyncService.fetchFullCvForUser()

        // Da (Assert)
        // Verifiser at kallene til WireMock ble gjort som forventet.
        verify(getRequestedFor(urlPathEqualTo("/v2/users/search")))
        verify(getRequestedFor(urlPathEqualTo("/v3/cvs/user123/cv456")))
    }
}