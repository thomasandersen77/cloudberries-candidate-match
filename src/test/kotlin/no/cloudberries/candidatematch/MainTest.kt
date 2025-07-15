package no.cloudberries.candidatematch

import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [Main::class])
@ActiveProfiles("test")
class MainTest {
    @Test
    fun start() {
        SpringApplication.run(Main::class.java)
    }
}
