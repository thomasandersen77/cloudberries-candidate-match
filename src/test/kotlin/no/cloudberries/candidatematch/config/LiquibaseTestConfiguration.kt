import liquibase.integration.spring.SpringLiquibase
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import javax.sql.DataSource

@TestConfiguration
@Import(LiquibaseTestConfig::class)
class LiquibaseTestConfig {
    @Bean
    fun liquibase(dataSource: DataSource): SpringLiquibase {
        return SpringLiquibase().apply {
            this.dataSource = dataSource
            this.changeLog = "classpath:db/changelog/db.changelog-master.xml"
        }
    }
}
