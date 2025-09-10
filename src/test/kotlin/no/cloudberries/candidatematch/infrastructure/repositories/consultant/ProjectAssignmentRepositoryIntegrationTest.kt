package no.cloudberries.candidatematch.infrastructure.repositories.consultant

import LiquibaseTestConfig
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.service.consultants.LiquidityReductionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@SpringBootTest
@Import(LiquibaseTestConfig::class)
class ProjectAssignmentRepositoryIntegrationTest {

    @Autowired lateinit var consultantRepository: ConsultantRepository
    @Autowired lateinit var projectAssignmentRepository: ProjectAssignmentRepository
    @Autowired lateinit var liquidityReductionService: LiquidityReductionService

    private val mapper = jacksonObjectMapper()

    @Test
    @Transactional
    fun `calculate liquidity reduction for non-billable assignment`() {
        val resumeJson: ObjectNode = mapper.createObjectNode().put("cv", "demo")
        val consultant = consultantRepository.save(
            ConsultantEntity(
                id = null,
                name = "Test Konsulent",
                userId = "user-123",
                cvId = "cv-123",
                resumeData = resumeJson,
                skills = mutableSetOf()
            )
        )

        val month = YearMonth.of(2025, 9)
        val start = month.atDay(1)
        val end = month.atEndOfMonth()

        // Non-billable 50% allocation, cost 100/h → expected 160*0.5*100 = 8000
        projectAssignmentRepository.save(
            no.cloudberries.candidatematch.infrastructure.entities.consultant.ProjectAssignmentEntity(
                id = null,
                consultant = consultant,
                title = "Internal project",
                startDate = start,
                endDate = end,
                allocationPercent = 50,
                hourlyRate = BigDecimal("0.00"),
                costRate = BigDecimal("100.00"),
                clientProjectRef = null,
                billable = false,
            )
        )

        // Billable assignment should not affect reduction
        projectAssignmentRepository.save(
            no.cloudberries.candidatematch.infrastructure.entities.consultant.ProjectAssignmentEntity(
                id = null,
                consultant = consultant,
                title = "Client A",
                startDate = start,
                endDate = end,
                allocationPercent = 50,
                hourlyRate = BigDecimal("1200.00"),
                costRate = BigDecimal("600.00"),
                clientProjectRef = "A-001",
                billable = true,
            )
        )

        val reduction = liquidityReductionService.calculateLiquidityReductionForMonth(consultant.id!!, month)
        assertEquals(BigDecimal("8000.00"), reduction)
    }
}

