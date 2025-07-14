import liquibase.integration.spring.SpringLiquibase
import no.cloudberries.candidatematch.config.EmbeddedPostgresTestConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import javax.sql.DataSource

@TestConfiguration
@Import(EmbeddedPostgresTestConfig::class)
class LiquibaseTestConfig {
    @Bean
    fun liquibase(@Qualifier("zonkyPostgresDatabaseProvider") postgresDataSource: DataSource): SpringLiquibase {
        return SpringLiquibase().apply {
            this.dataSource = postgresDataSource
            this.changeLog = "classpath:db/changelog/db.changelog-master.xml"
            this.contexts = "test"
        }
    }
}
