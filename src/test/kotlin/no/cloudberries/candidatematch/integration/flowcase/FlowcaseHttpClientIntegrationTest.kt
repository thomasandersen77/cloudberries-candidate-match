package no.cloudberries.candidatematch.integration.flowcase

import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore("Only for manual testing")
@SpringBootTest
class FlowcaseHttpClientIntegrationTest {
    @Autowired lateinit var flowcaseHttpClient: FlowcaseHttpClient

    @Test
    fun fetchAllCvs() {
        val response = flowcaseHttpClient.fetchFullCvById()
        assertNotNull(response)
    }

}