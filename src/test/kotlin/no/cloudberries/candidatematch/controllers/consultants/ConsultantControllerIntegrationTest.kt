package no.cloudberries.candidatematch.controllers.consultants

import LiquibaseTestConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.cloudberries.candidatematch.controllers.consultants.dto.AssignmentDto
import no.cloudberries.candidatematch.controllers.consultants.dto.CvDto
import no.cloudberries.candidatematch.controllers.consultants.dto.YearMonthPeriodDto
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ConsultantCvRepository
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ProjectAssignmentRepository
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.delete
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@Import(LiquibaseTestConfig::class)
class ConsultantControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var consultantRepository: ConsultantRepository
    @Autowired lateinit var consultantCvRepository: ConsultantCvRepository
    @Autowired lateinit var projectAssignmentRepository: ProjectAssignmentRepository

    private fun createConsultant(name: String, userId: String, cvId: String = "cv-$userId"): ConsultantEntity {
        val resumeJson: ObjectNode = objectMapper.createObjectNode().put("cv", cvId)
        return consultantRepository.save(
            ConsultantEntity(
                id = null,
                name = name,
                userId = userId,
                cvId = cvId,
                resumeData = resumeJson,
                skills = mutableSetOf()
            )
        )
    }

    @Test
    fun `list consultants with name filter and pagination`() {
        val c1 = createConsultant("Anna Andersson", "user-1")
        val c2 = createConsultant("Bob Builder", "user-2")

        mockMvc.get("/api/consultants") {
            param("name", "ann")
            param("page", "0")
            param("size", "10")
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.content", hasSize<Any>(1)) }
            .andExpect { jsonPath("$.content[0].name", equalTo("Anna Andersson")) }
            .andExpect { jsonPath("$.totalElements", equalTo(1)) }
    }

    @Test
    fun `create and list CVs with active filter`() {
        val c = createConsultant("Charlie", "user-3")
        // Create CV via API
        val cvReq = CvDto(
            id = null,
            versionTag = "v1",
            active = false,
            qualityScore = 80,
            keyQualifications = emptyList(),
            educations = emptyList(),
            workExperiences = emptyList(),
            projectExperiences = emptyList(),
            certifications = emptyList(),
            courses = emptyList(),
            languages = emptyList(),
            skillCategories = emptyList(),
            attachments = emptyList()
        )
        val createResult = mockMvc.post("/api/consultants/${c.id}/cvs") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(cvReq)
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.versionTag", equalTo("v1")) }
            .andReturn()

        val createdId: Long = objectMapper.readTree(createResult.response.contentAsString).get("id").asLong()

        // Activate it
        mockMvc.patch("/api/consultants/${c.id}/cvs/$createdId/activate") {}
            .andExpect { status { isOk() } }

        // List only active
        mockMvc.get("/api/consultants/${c.id}/cvs") {
            param("active", "true")
            param("page", "0")
            param("size", "10")
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.content", hasSize<Any>(1)) }
            .andExpect { jsonPath("$.content[0].active", equalTo(true)) }
    }

    @Test
    fun `create update delete assignments and filter`() {
        val c = createConsultant("Dina", "user-4")
        // Create non-billable Internal assignment
        val a1 = AssignmentDto(
            id = null,
            title = "Internal project",
            startDate = LocalDate.of(2025, 9, 1),
            endDate = LocalDate.of(2025, 9, 30),
            allocationPercent = 50,
            hourlyRate = BigDecimal("0.00"),
            costRate = BigDecimal("100.00"),
            clientProjectRef = null,
            billable = false
        )
        val createdA1 = mockMvc.post("/api/consultants/${c.id}/assignments") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(a1)
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.title", equalTo("Internal project")) }
            .andReturn()
        val a1Id = objectMapper.readTree(createdA1.response.contentAsString).get("id").asLong()

        // Create billable Client assignment
        val a2 = AssignmentDto(
            id = null,
            title = "Client A",
            startDate = LocalDate.of(2025, 9, 1),
            endDate = LocalDate.of(2025, 9, 30),
            allocationPercent = 50,
            hourlyRate = BigDecimal("1200.00"),
            costRate = BigDecimal("600.00"),
            clientProjectRef = "A-001",
            billable = true
        )
        mockMvc.post("/api/consultants/${c.id}/assignments") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(a2)
        }
            .andExpect { status { isOk() } }

        // Filter: billable=false and title contains 'internal'
        mockMvc.get("/api/consultants/${c.id}/assignments") {
            param("billable", "false")
            param("title", "internal")
            param("page", "0"); param("size", "10")
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.content", hasSize<Any>(1)) }
            .andExpect { jsonPath("$.content[0].title", equalTo("Internal project")) }

        // Update a1
        val a1Update = a1.copy(title = "Updated Internal")
        mockMvc.put("/api/consultants/${c.id}/assignments/$a1Id") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(a1Update)
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.title", equalTo("Updated Internal")) }

        // Verify filter by updated title
        mockMvc.get("/api/consultants/${c.id}/assignments") {
            param("title", "updated")
            param("page", "0"); param("size", "10")
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.content", hasSize<Any>(1)) }
            .andExpect { jsonPath("$.content[0].title", equalTo("Updated Internal")) }

        // Delete and verify empty
        mockMvc.delete("/api/consultants/${c.id}/assignments/$a1Id") {}
            .andExpect { status { isOk() } }

        mockMvc.get("/api/consultants/${c.id}/assignments") {
            param("title", "updated")
            param("page", "0"); param("size", "10")
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.content", hasSize<Any>(0)) }
    }
}

