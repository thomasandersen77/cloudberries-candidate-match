package no.cloudberries.candidatematch.integration.flowcase

import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest
class FlowcaseHttpClientIntegrationTest {
    @Autowired lateinit var flowcaseHttpClient: FlowcaseHttpClient

    @Test
    fun fetchAllCvs() {
        val response = flowcaseHttpClient.fetchAllCvs()
        assertNotNull(response)
        assertTrue(response.flowcaseCvDTOList.isNotEmpty())
        response.flowcaseCvDTOList.forEach {
            println(it)
        }
    }

}