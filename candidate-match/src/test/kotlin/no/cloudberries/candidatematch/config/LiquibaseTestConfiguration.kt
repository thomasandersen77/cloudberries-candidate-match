import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import liquibase.integration.spring.SpringLiquibase
import no.cloudberries.candidatematch.config.EmbeddedPostgresTestConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.io.DefaultResourceLoader
import javax.sql.DataSource

@TestConfiguration
@org.springframework.context.annotation.Profile("test")
@Import(EmbeddedPostgresTestConfig::class)
class LiquibaseTestConfig {
    @Bean
    fun liquibase(@Qualifier("zonkyPostgresDatabaseProvider") postgresDataSource: DataSource): SpringLiquibase {
        return SpringLiquibase().apply {
            this.dataSource = postgresDataSource
            this.changeLog = "classpath:db/changelog/db.changelog-master.xml"
            this.contexts = "test"
            this.defaultSchema = "public"
            // Add these lines to explicitly set the user credentials
            this.resourceLoader = DefaultResourceLoader()
            this.setShouldRun(true)
        }
    }
}