package no.cloudberries.candidatematch

import org.springframework.boot.SpringApplication.run
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.scheduling.annotation.EnableScheduling

@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication(scanBasePackages = ["no.cloudberries.candidatematch", "no.cloudberries.ai"])
@EnableScheduling
@org.springframework.scheduling.annotation.EnableAsync
@ConfigurationPropertiesScan
class Main

fun main(args: Array<String>) {
    run(Main::class.java, *args)
}
