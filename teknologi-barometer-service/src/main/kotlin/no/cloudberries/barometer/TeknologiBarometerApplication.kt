package no.cloudberries.barometer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TeknologiBarometerApplication

fun main(args: Array<String>) {
    runApplication<TeknologiBarometerApplication>(*args)
}