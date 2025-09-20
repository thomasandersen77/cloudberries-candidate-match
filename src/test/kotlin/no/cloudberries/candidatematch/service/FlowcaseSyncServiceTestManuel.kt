package no.cloudberries.candidatematch.service

import LiquibaseTestConfig
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.test.runTest
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.toDomain
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@Ignore("Only for manual testing")
@SpringBootTest
@ActiveProfiles("integration")
@Import(LiquibaseTestConfig::class)
class FlowcaseSyncServiceTestManuel {

    @Autowired
    lateinit var flowcaseHttpClient: FlowcaseHttpClient

    @Test
    fun fetchUsers() {
        val users = flowcaseHttpClient.fetchAllUsers().flowcaseUserDTOs
        assertTrue { users.isNotEmpty() && users.size > 100 }
    }

    val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Test
    @Disabled("Only for manual testing - not even triggered by run all tests in `IntelliJ`, To run, run just this test alone.")
    fun fetchFullCvForUsers() = runTest {


        val allUsers = flowcaseHttpClient.fetchAllUsers().flowcaseUserDTOs

        println("Fetched ${allUsers.size} users")

        var count = 0
        var filenumber = 1
        allUsers.forEachIndexed { index, it ->
            val cv = flowcaseHttpClient.fetchCompleteCv(
                it.userId,
                it.cvId
            )

            if (count % 10 == 0) {
                filenumber++
            }

            println(
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cv.toDomain())
            )

            val filename = "cvs_$filenumber.txt"
            try {
                Files.createFile(Paths.get(filename))
            } catch (e: Exception) {
                // ignore if exists
                println("File exists: $e   ---  IGNORING")
            }

            Files.writeString(
                Paths.get(filename),
                mapper.writeValueAsString(cv.toDomain()) + "\n",
                Charsets.UTF_8,
                StandardOpenOption.APPEND,
            )

            count = index + 1
        }
    }
}
