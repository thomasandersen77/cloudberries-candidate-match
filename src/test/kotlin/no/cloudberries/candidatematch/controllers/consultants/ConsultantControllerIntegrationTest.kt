package no.cloudberries.candidatematch.controllers.consultants

import LiquibaseTestConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ConsultantCvRepository
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ProjectAssignmentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
@Import(LiquibaseTestConfig::class)
class ConsultantControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var objectMapper: ObjectMapper
    @Autowired
    lateinit var consultantRepository: ConsultantRepository
    @Autowired
    lateinit var consultantCvRepository: ConsultantCvRepository
    @Autowired
    lateinit var projectAssignmentRepository: ProjectAssignmentRepository

    private fun createConsultant(name: String, userId: String, cvId: String = "cv-$userId"): ConsultantEntity {
        val resumeJson: ObjectNode = objectMapper.createObjectNode().put(
            "cv",
            cvId
        )
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
    fun `dummy smoke test to ensure context with new mappings starts`() {
        val c = createConsultant(
            "Smoke",
            "x-1"
        )
        assertEquals(
            "Smoke",
            c.name
        )
    }
}
