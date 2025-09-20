package no.cloudberries.candidatematch.controllers.consultants

import LiquibaseTestConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
@Import(LiquibaseTestConfig::class)
class ConsultantSearchControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var objectMapper: ObjectMapper
    @Autowired
    lateinit var consultantRepository: ConsultantRepository
    @Autowired
    lateinit var consultantSearchController: ConsultantSearchController

    @BeforeEach
    fun setup() {
        consultantRepository.deleteAll()
        fun createConsultant(name: String, userId: String, vararg skills: Skill) {
            val resume: ObjectNode = objectMapper.createObjectNode().put(
                "cv",
                "cv-$userId"
            )
            consultantRepository.save(
                no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity(
                    id = null,
                    name = name,
                    userId = userId,
                    cvId = "cv-$userId",
                    resumeData = resume,
                    skills = skills.toSet()
                )
            )
        }
        createConsultant(
            "Alice Kotlin",
            "u1",
            Skill.KOTLIN,
            Skill.BACKEND
        )
        createConsultant(
            "Bob Java",
            "u2",
            Skill.JAVA,
            Skill.BACKEND
        )
        createConsultant(
            "Cara React",
            "u3",
            Skill.REACT,
            Skill.FRONTEND
        )
    }

    @Test
    fun relational_basic_name_filter() {
        val result = consultantSearchController.searchRelational(
            ConsultantSearchController.RelationalSearchRequest(name = "kotlin"),
            PageRequest.of(
                0,
                10
            )
        )
        assertEquals(
            1,
            result.totalElements
        )
        assertEquals(
            "Alice Kotlin",
            result.content.first().name
        )
    }
}
