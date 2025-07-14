package no.cloudberries.candidatematch.integration.flowcase

import LiquibaseTestConfig
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.cloudberries.candidatematch.repositories.ProjectRequestRepository
import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore("Only for manual testing")
@Import(LiquibaseTestConfig::class)
@SpringBootTest
class FlowcaseHttpClientIntegrationTest {
    @Autowired
    lateinit var flowcaseHttpClient: FlowcaseHttpClient
    @MockBean
    lateinit var projectRequestRepository: ProjectRequestRepository
    val mapper = jacksonObjectMapper().apply {
        writerWithDefaultPrettyPrinter()
    }

    @Test
    fun fetchAllCvs() {
        val userid = "682c529a17774f004390031f"
        val cvId = "682c529acf99685aed6fd592"
        val response = flowcaseHttpClient.fetchFullCvById(userid, cvId)
        assertNotNull(response)
        println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response))
    }
}