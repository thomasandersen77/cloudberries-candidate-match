package no.cloudberries.candidatematch.infrastructure.adapters

import LiquibaseTestConfig
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Disabled("Only for manual testing")
@Import(LiquibaseTestConfig::class)
class ConsultantAdapterImplTest {

    @Autowired
    lateinit var consultantAdapter: ConsultantAdapterImpl

    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .writerWithDefaultPrettyPrinter()

    @Test
    fun fetchConsultantsWithCv() = runBlocking {
        consultantAdapter.fetchConsultantsWithCv()
            .forEach { println(it) }

    }


    @Test
    fun fetchSingleConsultant() {
        // Fetched user Thomas Andersen with userId 682c529a17774f004390031f and cvId 682c529acf99685aed6fd592
        // Could not fetch CV for Ailin Claussen with userId 6050cbfe4a3f780ef0b0b555 / cvId 6050cbfe4a3f780ef0b0b556.
        // Could not fetch CV for Alejandro Saksida with userId 67e3f430268875004add5354 / cvId 67e3f4302573b9d8e23250ca.
        // Could not fetch CV for Alejandro Saksida with userId 67e27aeb4d749c0040dd0206 / cvId 67e27aeb4e4f14fd3b9eb271.
        // Could not fetch CV for Aleksander Malde with userId 6888847cccd1c0003b9f9fff / cvId 6888847cccd1c0003b9fa002.
        // Could not fetch CV for Alf Petter Mossevig with userId 64bd970c8e64e90f8c1fb8e7 / cvId 64bd970cc33ab65f40637642.
        // Could not fetch CV for Anastasia Melås with userId 61b87b3b49e9771123774767 / cvId 61b87b3b49e9771123774768.
        // Could not fetch CV for Anders Helling with userId 5cc76b370788f54ad2742fe6 / cvId 5cc76b370788f54ad2742fe7.

        val userid = "6888847cccd1c0003b9f9fff"
        val cvId = "6888847cccd1c0003b9fa002"

        val completeCv = consultantAdapter.fetchCompleteCv(
            userid,
            cvId
        )

        println(mapper.writeValueAsString(completeCv))
    }
}