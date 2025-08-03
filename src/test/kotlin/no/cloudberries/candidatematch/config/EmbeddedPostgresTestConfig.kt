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
        val postgres = EmbeddedPostgres.builder()
            .start()

        // Create the test database before returning the DataSource
        postgres.postgresDatabase.connection.use { connection ->
            connection.createStatement().execute("""
                SELECT 'CREATE DATABASE test' 
                WHERE NOT EXISTS (
                    SELECT FROM pg_database WHERE datname = 'test'
                )
            """.trimIndent())
        }

        return postgres.getPostgresDatabase(mapOf(
            "user" to "postgres",
            "password" to "postgres"
        ))
    }
}