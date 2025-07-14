package no.cloudberries.candidatematch.config

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

/**
 * Test-konfigurasjon som setter opp og starter en embedded PostgreSQL-database
 * for bruk i integrasjonstester. Dette sikrer at testdatabasen er identisk
 * med databasen som brukes i produksjon.
 *
 * Denne konfigurasjonen definerer en DataSource-bean som peker mot den
 * embedded databasen. Spring Boot vil automatisk plukke opp denne og
 * overstyre produksjons-DataSource-konfigurasjonen når denne
 * test-konfigurasjonen er aktiv.
 */
@Configuration
class EmbeddedPostgresTestConfig {

    /**
     * Oppretter og konfigurerer en DataSource som kobler seg til en unik,
     * embedded PostgreSQL-database for hver testkontekst.
     *
     * @return En konfigurert DataSource-instans.
     */
    @Primary
    @Bean("zonkyPostgresDatabaseProvider")
    fun postgresDataSource(): DataSource {
        // Bygger og starter en embedded Postgres-instans.
        // '.start()' returnerer en ferdig konfigurert DataSource.
        // Databasen vil automatisk bli stengt ned når Spring-konteksten ødelegges.
        return EmbeddedPostgres.builder()
            .start().postgresDatabase
    }
}