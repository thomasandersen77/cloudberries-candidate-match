package no.cloudberries.candidatematch.integration.openai

import BaseIntegrationTest
import LiquibaseTestConfig
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.service.CandidateMatchingService
import no.cloudberries.candidatematch.utils.PdfUtils
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.io.FileInputStream
import kotlin.test.Ignore

@Ignore("Only for manual testing")
@SpringBootTest
@ActiveProfiles("test")
@Import(LiquibaseTestConfig::class)
class CandidateMatchingServiceIntegrationTest: BaseIntegrationTest() {
    @Autowired
    lateinit var candidateMatchingService: CandidateMatchingService
    @Autowired
    lateinit var flowcaseHttpClient: FlowcaseHttpClient

    val mapper = jacksonObjectMapper().apply {
        writerWithDefaultPrettyPrinter()
    }

    private val logger = KotlinLogging.logger {}
    @Test
    fun  matchCandidateOpenAI() {
        val response = candidateMatchingService.matchCandidate(
            aiProvider = AIProvider.OPENAI,
            cv = PdfUtils.extractText(FileInputStream(File("src/test/resources/Thomas-Andersen_CV.pdf"))),
            request = PdfUtils.extractText(FileInputStream(File("src/test/resources/politiet/forespørsel_fra_polititet.pdf"))),
            consultantName = "Thomas Andersen"
        )

        println("--- Candidate Match Response ---")
        println(
            """"          
        Score:     ${response.totalScore}
        Summary:   ${response.summary}
        """.trimIndent()
        )

        println("--- Matching Details ---")
        response.requirements.forEach { requirement ->

            println(
                """
               requirement: ${requirement.name}
               comment    : ${requirement.comment}
               score      : ${requirement.score}
            """.trimIndent()
            )
        }
    }

    @Test
    fun matchCandidateGemini() {
        val userid = "682c529a17774f004390031f"
        val cvId = "682c529acf99685aed6fd592"

        val resumeDTO = flowcaseHttpClient.fetchCompleteCv(userid, cvId)

        assertNotNull(resumeDTO)
        val resumeAsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resumeDTO)
        logger.info { resumeAsJson }
        val response = candidateMatchingService.matchCandidate(
            aiProvider = AIProvider.GEMINI,
            cv = resumeAsJson,
            //cv = PdfUtils.extractText(FileInputStream(File("src/test/resources/Thomas-Andersen_CV.pdf"))),
            //request = PdfUtils.extractText(FileInputStream(File("src/test/resources/politiet/forespørsel_fra_polititet.pdf"))),
            request = PdfUtils.extractText(FileInputStream(File("src/test/resources/politiet_arkitektur.pdf"))),
            consultantName = "Thomas Andersen"
        )

        println("--- Candidate Match Response ---")
        println(
            """"          
        Score:     ${response.totalScore}
        Summary:   ${response.summary}
        """.trimIndent()
        )

        println("--- Matching Details ---")
        response.requirements.forEach { requirement ->

            println(
                """
               requirement: ${requirement.name}
               comment    : ${requirement.comment}
               score      : ${requirement.score}
            """.trimIndent()
            )
        }
    }
}