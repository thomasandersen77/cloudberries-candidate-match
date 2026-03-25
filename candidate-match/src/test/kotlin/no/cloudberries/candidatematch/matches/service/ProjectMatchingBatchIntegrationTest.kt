package no.cloudberries.candidatematch.matches.service

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

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
@Disabled("Legacy GeminiFilesMatchingService has been removed. This test is no longer relevant for this module.")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProjectMatchingBatchIntegrationTest {

    @Test
    fun dummyTest() {
        // This is a placeholder to keep the test class valid but empty
    }
}
