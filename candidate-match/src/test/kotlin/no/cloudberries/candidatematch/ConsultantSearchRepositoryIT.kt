package no.cloudberries.candidatematch

import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantSearchRepository
import no.cloudberries.candidatematch.config.SearchLexicon
import no.cloudberries.candidatematch.config.SearchLexiconProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class ConsultantSearchRepositoryIT {

    @Test
    fun `reRankBySemanticSimilarity orders by distance ascending`() {
        val log = LoggerFactory.getLogger(ConsultantSearchRepositoryIT::class.java)
        val image = DockerImageName.parse("pgvector/pgvector:pg15")
        PostgreSQLContainer<Nothing>(image).use { pg ->
            pg.start()
            val ds = DriverManagerDataSource(pg.jdbcUrl, pg.username, pg.password)
            val jdbc = JdbcTemplate(ds)
            // Enable pgvector
            jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector;")
            // Minimal schema
            jdbc.execute(
                """
                CREATE TABLE consultant (
                  id BIGSERIAL PRIMARY KEY,
                  user_id VARCHAR(255) NOT NULL,
                  name VARCHAR(255) NOT NULL,
                  cv_id VARCHAR(255) NOT NULL
                );
                """.trimIndent()
            )
            jdbc.execute(
                """
                CREATE TABLE consultant_cv (
                  id BIGSERIAL PRIMARY KEY,
                  consultant_id BIGINT NOT NULL,
                  quality_score INT,
                  active BOOLEAN DEFAULT true
                );
                """.trimIndent()
            )
            jdbc.execute(
                """
                CREATE TABLE cv_embedding (
                  id BIGSERIAL PRIMARY KEY,
                  user_id VARCHAR(255) NOT NULL,
                  cv_id VARCHAR(255) NOT NULL,
                  provider VARCHAR(50) NOT NULL,
                  model VARCHAR(100) NOT NULL,
                  embedding VECTOR(3) NOT NULL
                );
                """.trimIndent()
            )
            // Data
            jdbc.update("INSERT INTO consultant(user_id,name,cv_id) VALUES (?,?,?)", "u1", "Alice", "cv1")
            jdbc.update("INSERT INTO consultant(user_id,name,cv_id) VALUES (?,?,?)", "u2", "Bob", "cv2")
            jdbc.update("INSERT INTO consultant_cv(consultant_id,quality_score,active) VALUES (?,?,true)", 1L, 80)
            jdbc.update("INSERT INTO consultant_cv(consultant_id,quality_score,active) VALUES (?,?,true)", 2L, 90)
            // embeddings
            jdbc.update(
                "INSERT INTO cv_embedding(user_id,cv_id,provider,model,embedding) VALUES (?,?,?,?,?::vector)",
                "u1", "cv1", "TEST", "M", "[0.0,0.0,1.0]"
            )
            jdbc.update(
                "INSERT INTO cv_embedding(user_id,cv_id,provider,model,embedding) VALUES (?,?,?,?,?::vector)",
                "u2", "cv2", "TEST", "M", "[1.0,0.0,0.0]"
            )

            val repo = ConsultantSearchRepository(jdbc, SearchLexicon(SearchLexiconProperties()))
            val query = doubleArrayOf(0.0, 0.0, 1.0) // identical to u1 vector
            val results = repo.reRankBySemanticSimilarity(
                embedding = query,
                provider = "TEST",
                model = "M",
                allowedPairs = listOf("u1" to "cv1", "u2" to "cv2"),
                topK = 2
            )
            // u1 should be first (distance 0)
            assertEquals("u1", results[0].userId)
            assertEquals("u2", results[1].userId)
        }
    }
}
