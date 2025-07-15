package no.cloudberries.candidatematch.config

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class EmbeddedPostgresTestConfig {

    @Primary
    @Bean("zonkyPostgresDatabaseProvider")
    fun postgresDataSource(): DataSource {
        return EmbeddedPostgres.builder()
            .start()
            .getPostgresDatabase(mapOf(
                "user" to "test_user",
                "password" to "test_password"
            ))
    }
}