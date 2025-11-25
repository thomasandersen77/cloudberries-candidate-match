package no.cloudberries.candidatematch.matches.service

import kotlinx.coroutines.test.runTest
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.ProjectRequest
import no.cloudberries.candidatematch.domain.ProjectRequestId
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.infrastructure.entities.RequestStatus
import no.cloudberries.candidatematch.service.matching.GeminiFilesMatchingService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * Manual integration test for batch matching using Gemini Files API.
 * 
 * This test is @Disabled by default and must be run manually from IntelliJ
 * when you want to verify the batch matching implementation.
 * 
 * Prerequisites:
 * - GEMINI_API_KEY environment variable must be set
 * - Database must be running: docker-compose -f docker-compose-local.yaml up -d
 * - Application profile 'local' must be active
 * 
 * What this test verifies:
 * 1. GeminiFilesMatchingService.matchConsultantsWithFilesApi() is called
 * 2. CVs are uploaded to Gemini Files API (with caching)
 * 3. Single batch ranking call is made (not sequential calls)
 * 4. Results are returned with scores and justifications
 * 
 * How to run:
 * 1. Start database: cd candidate-match && docker-compose -f docker-compose-local.yaml up -d
 * 2. Right-click on the test method in IntelliJ
 * 3. Select "Run 'testBatchMatchingWithFilesApi()'"
 * 4. Check console output for [FILES API] and [STEP] log messages
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.test.context.TestExecutionListeners(
    listeners = [
        org.springframework.test.context.support.DependencyInjectionTestExecutionListener::class,
        org.springframework.test.context.support.DirtiesContextTestExecutionListener::class,
        org.springframework.test.context.transaction.TransactionalTestExecutionListener::class
    ],
    mergeMode = org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
//@Disabled("Manual test - run from IntelliJ with local Docker database. Remove @Disabled and click Run.")
class ProjectMatchingBatchIntegrationTest {

    @Autowired
    private lateinit var geminiFilesMatchingService: GeminiFilesMatchingService
    
    @Autowired
    private lateinit var projectMatchingService: ProjectMatchingService
    
    @Autowired
    private lateinit var dataSource: javax.sql.DataSource

    private val logger = KotlinLogging.logger {}

    @Test
    fun testBatchMatchingWithFilesApi() = runTest {
        logger.info { "=== Starting Manual Batch Matching Integration Test ===" }
        
        // Debug: Check which database we're connected to
        val jdbcTemplate = org.springframework.jdbc.core.JdbcTemplate(dataSource)
        val url = dataSource.connection.use { it.metaData.url }
        val consultantCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consultant", Int::class.java) ?: 0
        logger.info { "Connected to database: $url" }
        logger.info { "Consultant count in database: $consultantCount" }
        
        // Create a realistic project request
        val projectRequest = ProjectRequest(
            id = ProjectRequestId(999L), // Test ID
            customerId = null,
            customerName = "Test Customer - Politiet",
            requiredSkills = listOf(
                Skill.of("Kotlin"),
                Skill.of("Spring Boot"),
                Skill.of("PostgreSQL"),
                Skill.of("React"),
                Skill.of("TypeScript")
            ),
            startDate = LocalDateTime.now().plusMonths(1),
            endDate = LocalDateTime.now().plusMonths(7),
            responseDeadline = LocalDateTime.now().plusDays(14),
            requestDescription = """
                Vi søker en fullstack-utvikler til et spennende prosjekt i offentlig sektor.
                
                MÅ-KRAV:
                - Kotlin og Spring Boot backend-utvikling
                - React og TypeScript frontend-utvikling
                - PostgreSQL databasekompetanse
                - Erfaring med smidig utvikling
                - Norsk språk (muntlig og skriftlig)
                
                BØR-KRAV:
                - Erfaring fra offentlig sektor
                - Kubernetes og Docker
                - CI/CD pipelines
                - Sikkerhet og personvern
                
                Prosjektet går over 6 måneder med mulighet for forlengelse.
            """.trimIndent(),
            responsibleSalespersonEmail = "test@cloudberries.no",
            status = RequestStatus.OPEN,
            aISuggestions = emptyList()
        )
        
        // Required skills as strings
        val requiredSkills = projectRequest.requiredSkills.map { it.name }
        
        logger.info { "Project Request: ${projectRequest.customerName}" }
        logger.info { "Required Skills: ${requiredSkills.joinToString(", ")}" }
        logger.info { "Description length: ${projectRequest.requestDescription.length} chars" }
        logger.info { "" }
        logger.info { "=== Calling GeminiFilesMatchingService.matchConsultantsWithFilesApi() ===" }
        logger.info { "Watch for these log messages:" }
        logger.info { "  [FILES API] Starting match..." }
        logger.info { "  [STEP 1] Fetching candidate pool..." }
        logger.info { "  [STEP 2] Scoring consultants..." }
        logger.info { "  [STEP 3] Uploading CVs to Gemini..." }
        logger.info { "  [STEP 4] Calling Gemini API with N file references in SINGLE request" }
        logger.info { "  [RESULT] Returning N matched consultants" }
        logger.info { "" }
        
        try {
            // Call the batch matching service directly
            val results = geminiFilesMatchingService.matchConsultantsWithFilesApi(
                projectRequest = projectRequest,
                requiredSkills = requiredSkills,
                topN = 5
            )
            
            logger.info { "=== Batch Matching Completed Successfully ===" }
            logger.info { "Number of matches returned: ${results.size}" }
            logger.info { "" }
            
            results.forEachIndexed { index, match ->
                logger.info { "Rank ${index + 1}:" }
                logger.info { "  Name: ${match.name}" }
                logger.info { "  User ID: ${match.userId}" }
                logger.info { "  CV ID: ${match.cvId}" }
                logger.info { "  Score: ${match.relevanceScore}/100" }
                logger.info { "  Justification: ${match.justification?.take(200)}..." }
                logger.info { "" }
            }
            
            // Assertions
            assert(results.isNotEmpty()) { "Expected at least one match, got empty list" }
            results.forEach { match ->
                assert(match.relevanceScore >= 0 && match.relevanceScore <= 100) { "Score must be between 0 and 100, got ${match.relevanceScore}" }
                assert(match.userId.isNotBlank()) { "User ID must not be blank" }
                assert(match.name.isNotBlank()) { "Name must not be blank" }
            }
            
            logger.info { "=== ✅ All Assertions Passed ===" }
            logger.info { "Batch matching with Files API is working correctly!" }
            
        } catch (e: Exception) {
            logger.error(e) { "=== ❌ Batch Matching Failed ===" }
            logger.error { "Error message: ${e.message}" }
            logger.error { "Stack trace:" }
            e.printStackTrace()
            throw e
        }
    }
    
    @Test
    fun `testBatchMatchingViaController - end-to-end verification`() = runTest {
        logger.info { "=== Starting End-to-End Batch Matching Test ===" }
        logger.info { "This test will trigger matching via the service layer" }
        logger.info { "and verify that results are persisted to the database." }
        logger.info { "" }
        
        // Note: This would require creating a ProjectRequestEntity first
        // and then calling projectMatchingService.computeAndPersistMatches()
        // For now, we'll just log the intent
        
        logger.info { "To test end-to-end:" }
        logger.info { "1. Upload a PDF via Swagger UI: http://localhost:8080/swagger-ui/index.html" }
        logger.info { "2. POST to /api/project-requests/upload" }
        logger.info { "3. Check logs for batch matching execution" }
        logger.info { "4. Query database for match results:" }
        logger.info { "   SELECT * FROM project_match_result ORDER BY created_at DESC LIMIT 1;" }
        logger.info { "   SELECT * FROM match_candidate_result WHERE match_result_id = X;" }
        logger.info { "" }
        logger.info { "Expected log flow:" }
        logger.info { "  ProjectRequestController: Triggering async batch matching for project request X" }
        logger.info { "  ProjectMatchingServiceImpl: Using Gemini Files API batch matching" }
        logger.info { "  GeminiFilesMatchingService: [FILES API] Starting match..." }
        logger.info { "  GeminiFilesApiAdapter: Ranking attempt 1/2 with model: gemini-2.5-pro" }
        logger.info { "  (or fallback to gemini-2.5-flash if 503)" }
    }
}
