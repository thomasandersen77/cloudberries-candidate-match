package no.cloudberries.candidatematch.config

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.postgresql.ds.PGSimpleDataSource
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

        // Ensure the default 'test' database exists (harmless if it already does)
        postgres.postgresDatabase.connection.use { connection ->
            connection.createStatement().execute(
                """
                SELECT 'CREATE DATABASE test'
                WHERE NOT EXISTS (
                    SELECT FROM pg_database WHERE datname = 'test'
                )
                """.trimIndent()
            )
        }

        // Build a DataSource that prefers IPv6 loopback to avoid rare IPv4 port-collision issues.
        val port = postgres.port
        return PGSimpleDataSource().apply {
            setServerNames(
                arrayOf(
                    "::1",
                    "127.0.0.1",
                    "localhost"
                )
            )
            setPortNumbers(
                intArrayOf(
                    port,
                    port,
                    port
                )
            )
            setDatabaseName("postgres")
            setUser("postgres")
            setPassword("postgres")
        }
    }
}
