package no.cloudberries.candidatematch

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class Main

fun main(args: Array<String>) {
    val context = SpringApplication.run(
        Main::class.java,
        *args
    )

    //context.getBean(SyncConsultantService::class.java).syncAll(120)
    // context.getBean(ScoreCandidateStartupRunner::class.java).run()

}
