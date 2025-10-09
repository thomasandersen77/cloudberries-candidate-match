package no.cloudberries.candidatematch.controllers.consultants

import LiquibaseTestConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.entities.consultant.*
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Import(LiquibaseTestConfig::class)
@ActiveProfiles("test")
@Transactional
class ConsultantCvQueryControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @Autowired lateinit var consultantRepository: ConsultantRepository
    @Autowired lateinit var consultantCvRepository: ConsultantCvRepository
    @Autowired lateinit var cvKeyQualificationRepository: CvKeyQualificationRepository
    @Autowired lateinit var cvProjectExperienceRepository: CvProjectExperienceRepository
    @Autowired lateinit var cvProjectExperienceRoleRepository: CvProjectExperienceRoleRepository
    @Autowired lateinit var cvProjectExperienceSkillRepository: CvProjectExperienceSkillRepository

    private fun createConsultantWithCv(userId: String, name: String, activeCv: Boolean = true): Pair<ConsultantEntity, ConsultantCvEntity> {
        val resumeJson: ObjectNode = objectMapper.createObjectNode().put("cv", "cv-$userId")
        val consultant = consultantRepository.save(
            ConsultantEntity(
                id = null,
                name = name,
                userId = userId,
                cvId = "cv-$userId",
                resumeData = resumeJson
            )
        )
        val cv = consultantCvRepository.save(
            ConsultantCvEntity(
                id = null,
                consultantId = consultant.id!!,
                versionTag = "v1",
                qualityScore = 80,
                active = activeCv
            )
        )
        return consultant to cv
    }

    @Test
    fun `should assemble consultant with nested cv`() {
        val (consultant, cv) = createConsultantWithCv("u-1", "Anna", activeCv = true)
        cvKeyQualificationRepository.save(
            CvKeyQualificationEntity(
                id = null,
                cvId = cv.id!!,
                label = "Core",
                description = "Senior Kotlin dev"
            )
        )
        val proj = cvProjectExperienceRepository.save(
            CvProjectExperienceEntity(
                id = null,
                cvId = cv.id!!,
                customer = "ACME",
                description = "Project desc",
                longDescription = "Long desc",
                fromYearMonth = "2024-01",
                toYearMonth = "2024-06"
            )
        )
        cvProjectExperienceRoleRepository.save(
            CvProjectExperienceRoleEntity(
                id = null,
                projectExperienceId = proj.id!!,
                name = "Developer",
                description = "Backend"
            )
        )
        cvProjectExperienceSkillRepository.save(
            CvProjectExperienceSkillEntity(
                id = null,
                projectExperienceId = proj.id!!,
                skill = "Kotlin"
            )
        )

        mockMvc.get("/consultants/with-cv?onlyActiveCv=true")
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$[0].userId") { value(consultant.userId) }
                jsonPath("$[0].name") { value("Anna") }
                jsonPath("$[0].cvs[0].active") { value(true) }
                jsonPath("$[0].cvs[0].keyQualifications[0].label") { value("Core") }
                jsonPath("$[0].cvs[0].projectExperience[0].roles[0].name") { value("Developer") }
                jsonPath("$[0].cvs[0].projectExperience[0].skills[0]") { value("Kotlin") }
            }
    }

    @Test
    fun `paged variant should return correct totals and page size`() {
        createConsultantWithCv("u-1", "Anna", activeCv = true)
        createConsultantWithCv("u-2", "Bob", activeCv = true)
        createConsultantWithCv("u-3", "Cara", activeCv = false)

        mockMvc.get("/consultants/with-cv/paged?page=0&size=2")
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.number") { value(0) }
                jsonPath("$.size") { value(2) }
                jsonPath("$.content.length()") { value(2) }
                jsonPath("$.totalElements") { value(3) }
            }
    }
}
