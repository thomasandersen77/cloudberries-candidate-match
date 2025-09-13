package no.cloudberries.candidatematch.infrastructure.repositories.embedding

import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer

@TestInstance(Lifecycle.PER_CLASS)
@Tag("integration")
@EnabledIfSystemProperty(
    named = "runPgVectorIT",
    matches = "true"
)
class CvEmbeddingRepositoryIT {

    private lateinit var postgres: PostgreSQLContainer<*>

    @BeforeAll
    fun startContainer() {
        postgres = PostgreSQLContainer("pgvector/pgvector:pg15").apply {
            withDatabaseName("test")
            withUsername("test")
            withPassword("test")
            withInitScript("db/changelog/it_init_pgvector.sql")
            start()
        }
    }

    @AfterAll
    fun stopContainer() {
        runCatching { postgres.stop() }
    }

    private fun jdbcTemplate(): JdbcTemplate {
        val ds = DriverManagerDataSource().apply {
            setDriverClassName("org.postgresql.Driver")
            url = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        }
        return JdbcTemplate(ds)
    }

    @Test
    fun `save and exists works against pgvector`() {
        val jdbc = jdbcTemplate()
        val repo = CvEmbeddingRepository(jdbc)

        val userId = "user1"
        val cvId = "cv1"
        val provider = "GOOGLE_GEMINI"
        val model = "text-embedding-004"
        val vec = DoubleArray(768) { i -> i / 1000.0 }

        repo.save(
            userId,
            cvId,
            provider,
            model,
            vec
        )
        val exists = repo.exists(
            userId,
            cvId,
            provider,
            model
        )
        Assertions.assertTrue(exists)

        val count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM cv_embedding WHERE user_id=?",
            Int::class.java,
            userId
        )
        Assertions.assertEquals(
            1,
            count
        )
    }
}
