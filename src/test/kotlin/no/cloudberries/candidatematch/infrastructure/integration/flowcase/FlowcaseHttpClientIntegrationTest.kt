package no.cloudberries.candidatematch.infrastructure.integration.flowcase

import LiquibaseTestConfig
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.infrastructure.repositories.ProjectRequestRepository
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Disabled("Only for manual testing")
@Import(LiquibaseTestConfig::class)
@ActiveProfiles("integration")
@SpringBootTest
class FlowcaseHttpClientIntegrationTest {
    @Autowired
    lateinit var flowcaseHttpClient: FlowcaseHttpClient

    @MockBean
    lateinit var projectRequestRepository: ProjectRequestRepository
    val mapper = jacksonObjectMapper().apply {
        writerWithDefaultPrettyPrinter()
    }.registerModule(JavaTimeModule())

    @Test
    fun fetchCompleteCv() {
        val userid = "682c529a17774f004390031f"
        val cvId = "682c529acf99685aed6fd592"
        val response = flowcaseHttpClient.fetchCompleteCv(
            userid,
            cvId
        )

        assertNotNull(response)

        println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response))
    }

    @Test
    fun fetchAllConsultantWithCompleteCV() = runTest {
        fetchAndPrintConsultantsCvs()
    }

    suspend fun fetchAndPrintConsultantsCvs() {
        flowcaseHttpClient.fetchAllUsers().flowcaseUserDTOs.forEach {
            delay(50)
            val cv = flowcaseHttpClient.fetchCompleteCv(
                it.userId,
                it.cvId
            )
            assertNotNull(cv)
            println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cv))
        }
    }
}