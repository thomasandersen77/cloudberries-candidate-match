package no.cloudberries.candidatematch.service.scoring

import LiquibaseTestConfig
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.cloudberries.candidatematch.domain.consultant.ConsultantAdapter
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Disabled("Not yet implemented")
@Import(LiquibaseTestConfig::class)
@ActiveProfiles("integration")
class ScoreCandidateServiceIntegrationTest {

    @Autowired
    lateinit var scoreCandidateService: ScoreCandidateService

    @Autowired
    lateinit var consultantAdapter: ConsultantAdapter

    private val logger = mu.KotlinLogging.logger {}
    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(
            FAIL_ON_UNKNOWN_PROPERTIES,
            false
        )
        .configure(
            FAIL_ON_NULL_FOR_PRIMITIVES,
            false
        )

    @Test
    fun `score candidates`() {
        val consultants = consultantAdapter.fetchAllConsultants()

        val consultantCvList = mutableListOf<CVEvaluation>()
        consultants.reversed().forEach {
            val consultant = consultantAdapter.fetchConsultant(
                it.userId,
            )
            logger.info { "Evaluate CV score for ${it.name}" }
            val cvEvaluation = scoreCandidateService.performCvScoring(
                cv = consultant.cvAsJson,
                consultantName = it.name
            )
            logger.info { "Finished evaluating CV score for ${it.name} - score: ${cvEvaluation.scorePercentage}" }
            consultantCvList.add(cvEvaluation)
            //logger.info { mapper.writeValueAsString(reviewResponseDto) }
            //Thread.sleep(100)
        }
        consultantCvList.sortByDescending { it.scorePercentage }
        consultantCvList.forEach {
            println(
                """
                ${it.scorePercentage} - ${it.name} - ${it.summary}
            """.trimIndent()
            )
        }
        println("finished")
    }
}

