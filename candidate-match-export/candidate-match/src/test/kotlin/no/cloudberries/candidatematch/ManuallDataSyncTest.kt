package no.cloudberries.candidatematch

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

@Disabled
@SpringBootTest
class ManuallDataSyncTest {

    @Test
    fun `uuid test for marit`() {
        val randomUUID = UUID.randomUUID()
        println(randomUUID)
    }
}