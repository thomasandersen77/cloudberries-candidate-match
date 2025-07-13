import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

class EmbeddedPostgresContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val postgres = EmbeddedPostgres.start()
        
        applicationContext.beanFactory.registerSingleton(
            "dataSource",
            postgres.postgresDatabase
        )
    }
}
