package no.cloudberries.candidatematch.integration.openai

import no.cloudberries.candidatematch.integration.AiProvider
import no.cloudberries.candidatematch.service.CandidateMatchingService
import no.cloudberries.candidatematch.utils.PdfUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.io.FileInputStream
import kotlin.test.Ignore

@Ignore("Only for manual testing")
@SpringBootTest
class CandidateMatchingServiceIntegrationTest {
    @Autowired
    lateinit var candidateMatchingService: CandidateMatchingService

    @Test
    fun matchCandidateOpenAI() {
        val response = candidateMatchingService.matchCandidate(
            aiProvider = AiProvider.OPENAI,
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
        val response = candidateMatchingService.matchCandidate(
            aiProvider = AiProvider.GEMINI,
            cv = PdfUtils.extractText(FileInputStream(File("src/test/resources/Thomas-Andersen_CV.pdf"))),
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