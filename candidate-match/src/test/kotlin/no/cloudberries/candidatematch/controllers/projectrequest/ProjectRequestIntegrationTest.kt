package no.cloudberries.candidatematch.controllers.projectrequest

import com.fasterxml.jackson.databind.ObjectMapper
import no.cloudberries.candidatematch.infrastructure.entities.RequestStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "ai.enabled=false", // Disable AI for integration tests
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
@Transactional
class ProjectRequestIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create project request via API`() {
        // Given
        val createRequest = CreateProjectRequestDto(
            customerName = "Test Customer AS",
            requiredSkills = listOf(no.cloudberries.candidatematch.domain.candidate.Skill.of("KOTLIN"), no.cloudberries.candidatematch.domain.candidate.Skill.of("JAVA")),
            startDate = LocalDateTime.now().plusDays(30),
            endDate = LocalDateTime.now().plusDays(90),
            responseDeadline = LocalDateTime.now().plusDays(20),
            requestDescription = "We need a senior backend developer for a 6-month project.",
            responsibleSalespersonEmail = "sales@testcompany.com"
        )

        val jsonContent = objectMapper.writeValueAsString(createRequest)

        // When & Then
        mockMvc.perform(
            post("/project-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.customerName").value("Test Customer AS"))
            .andExpect(jsonPath("$.requiredSkills").isArray)
            .andExpect(jsonPath("$.requiredSkills[0]").value("KOTLIN"))
            .andExpect(jsonPath("$.requiredSkills[1]").value("JAVA"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.requestDescription").value("We need a senior backend developer for a 6-month project."))
            .andExpect(jsonPath("$.responsibleSalespersonEmail").value("sales@testcompany.com"))
    }

    @Test
    fun `should reject invalid project request`() {
        // Given - Invalid request with blank customer name
        val invalidRequest = CreateProjectRequestDto(
            customerName = "",
            requiredSkills = listOf(no.cloudberries.candidatematch.domain.candidate.Skill.of("KOTLIN")),
            startDate = LocalDateTime.now().plusDays(30),
            endDate = LocalDateTime.now().plusDays(90),
            responseDeadline = LocalDateTime.now().plusDays(20),
            requestDescription = "Test description",
            responsibleSalespersonEmail = "invalid-email"
        )

        val jsonContent = objectMapper.writeValueAsString(invalidRequest)

        // When & Then
        mockMvc.perform(
            post("/project-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should list project requests with pagination`() {
        // Given - Create a project request first
        val createRequest = CreateProjectRequestDto(
            customerName = "Test Customer",
            requiredSkills = listOf(no.cloudberries.candidatematch.domain.candidate.Skill.of("KOTLIN")),
            startDate = LocalDateTime.now().plusDays(30),
            endDate = LocalDateTime.now().plusDays(90),
            responseDeadline = LocalDateTime.now().plusDays(20),
            requestDescription = "Test project",
            responsibleSalespersonEmail = "test@example.com"
        )

        mockMvc.perform(
            post("/project-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )

        // When & Then
        mockMvc.perform(
            get("/project-requests")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "id,desc")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentPage").value(0))
            .andExpect(jsonPath("$.pageSize").value(10))
            .andExpected(jsonPath("$.content").isArray)
    }

    @Test
    fun `should get project request by id`() {
        // Given - Create a project request first
        val createRequest = CreateProjectRequestDto(
            customerName = "Test Customer",
            requiredSkills = listOf(no.cloudberries.candidatematch.domain.candidate.Skill.of("KOTLIN")),
            startDate = LocalDateTime.now().plusDays(30),
            endDate = LocalDateTime.now().plusDays(90),
            responseDeadline = LocalDateTime.now().plusDays(20),
            requestDescription = "Test project",
            responsibleSalespersonEmail = "test@example.com"
        )

        val result = mockMvc.perform(
            post("/project-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val responseContent = result.response.contentAsString
        val createdRequest = objectMapper.readValue(responseContent, ProjectRequestDto::class.java)
        val projectId = createdRequest.id!!

        // When & Then
        mockMvc.perform(get("/project-requests/$projectId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(projectId))
            .andExpect(jsonPath("$.customerName").value("Test Customer"))
    }

    @Test
    fun `should close project request`() {
        // Given - Create a project request first
        val createRequest = CreateProjectRequestDto(
            customerName = "Test Customer",
            requiredSkills = listOf(no.cloudberries.candidatematch.domain.candidate.Skill.of("KOTLIN")),
            startDate = LocalDateTime.now().plusDays(30),
            endDate = LocalDateTime.now().plusDays(90),
            responseDeadline = LocalDateTime.now().plusDays(20),
            requestDescription = "Test project",
            responsibleSalespersonEmail = "test@example.com"
        )

        val result = mockMvc.perform(
            post("/project-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val responseContent = result.response.contentAsString
        val createdRequest = objectMapper.readValue(responseContent, ProjectRequestDto::class.java)
        val projectId = createdRequest.id!!

        // When & Then
        mockMvc.perform(put("/project-requests/$projectId/close"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(projectId))
            .andExpected(jsonPath("$.status").value("CLOSED"))
    }

    @Test
    fun `should return 404 for non-existent project request`() {
        // When & Then
        mockMvc.perform(get("/project-requests/999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should get empty suggestions for new project request without AI`() {
        // Given - Create a project request first
        val createRequest = CreateProjectRequestDto(
            customerName = "Test Customer",
            requiredSkills = listOf(no.cloudberries.candidatematch.domain.candidate.Skill.of("KOTLIN")),
            startDate = LocalDateTime.now().plusDays(30),
            endDate = LocalDateTime.now().plusDays(90),
            responseDeadline = LocalDateTime.now().plusDays(20),
            requestDescription = "Test project",
            responsibleSalespersonEmail = "test@example.com"
        )

        val result = mockMvc.perform(
            post("/project-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val responseContent = result.response.contentAsString
        val createdRequest = objectMapper.readValue(responseContent, ProjectRequestDto::class.java)
        val projectId = createdRequest.id!!

        // When & Then
        mockMvc.perform(get("/project-requests/$projectId/suggestions"))
            .andExpected(status().isOk)
            .andExpected(jsonPath("$").isArray)
            .andExpected(jsonPath("$").isEmpty)
    }
}

// Extension function for better test readability
private fun org.springframework.test.web.servlet.ResultActions.andExpected(matcher: org.springframework.test.web.servlet.ResultMatcher): org.springframework.test.web.servlet.ResultActions {
    return this.andExpect(matcher)
}