package no.cloudberries.candidatematch.infrastructure.integration.openai

import LiquibaseTestConfig
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.CandidateMatchResponse
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.service.matching.CandidateMatchingService
import no.cloudberries.candidatematch.utils.PdfUtils
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.io.FileInputStream

@Disabled("Only for manual testing")
@SpringBootTest
@ActiveProfiles("manualtest")
@Import(LiquibaseTestConfig::class)
class CandidateMatchingServiceIntegrationTest {
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
        val userid = "682c529a17774f004390031f"
        val cvId = "682c529acf99685aed6fd592"

        val resumeDTO = flowcaseHttpClient.fetchCompleteCv(userid, cvId)

        assertNotNull(resumeDTO)
        val resumeAsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resumeDTO)

        val aiProvider = AIProvider.OPENAI
        val response = candidateMatchingService.matchCandidate(
            aiProvider = aiProvider,
            cv = resumeAsJson,
            request = PdfUtils.extractText(FileInputStream(File("src/test/resources/politiet/forespørsel_fra_polititet.pdf"))),
            consultantName = "Thomas Andersen"
        )

        printFormattedResponse(
            aiProvider,
            response
        )
    }

    @Test
    fun matchCandidateGemini() {
        val userid = "682c529a17774f004390031f"
        val cvId = "682c529acf99685aed6fd592"

        val resumeDTO = flowcaseHttpClient.fetchCompleteCv(userid, cvId)

        assertNotNull(resumeDTO)
        val resumeAsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resumeDTO)
        logger.info { resumeAsJson }
        val aiProvider = AIProvider.GEMINI
        val response = candidateMatchingService.matchCandidate(
            aiProvider = aiProvider,
            cv = resumeAsJson,
            request = PdfUtils.extractText(FileInputStream(File("src/test/resources/politiet_arkitektur.pdf"))),
            consultantName = "Thomas Andersen"
        )

        printFormattedResponse(
            aiProvider,
            response
        )
    }

    private fun printFormattedResponse(
        aiProvider: AIProvider,
        response: CandidateMatchResponse
    ) {
        println("=".repeat(80))
        println("CANDIDATE MATCH RESPONSE FROM $aiProvider")
        println("=".repeat(80))
        println("Score:     ${response.totalScore}")
        println()
        println("Summary:")
        println("-".repeat(40))
        println(
            wrapText(
                response.summary,
                80
            )
        )
        println()

        println("=".repeat(80))
        println("MATCHING DETAILS")
        println("=".repeat(80))
        response.requirements.forEach { requirement ->
            println()
            println("Requirement: ${requirement.name}")
            println("Score:       ${requirement.score}/10")
            println("Comment:")
            println("-".repeat(40))
            println(
                wrapText(
                    requirement.comment,
                    80
                )
            )
            println("-".repeat(80))
        }
    }

    /**
     * Wraps text to specified line length for better console readability
     */
    private fun wrapText(text: String, lineLength: Int): String {
        val words = text.split(" ")
        val result = StringBuilder()
        var currentLine = StringBuilder()
        
        for (word in words) {
            if (currentLine.length + word.length + 1 > lineLength) {
                if (currentLine.isNotEmpty()) {
                    result.appendLine(currentLine.toString())
                    currentLine = StringBuilder()
                }
            }
            
            if (currentLine.isNotEmpty()) {
                currentLine.append(" ")
            }
            currentLine.append(word)
        }
        
        if (currentLine.isNotEmpty()) {
            result.append(currentLine.toString())
        }
        
        return result.toString()
    }
}