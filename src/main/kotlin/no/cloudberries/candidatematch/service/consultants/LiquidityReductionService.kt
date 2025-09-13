package no.cloudberries.candidatematch.service.consultants

import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ProjectAssignmentRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

@Service
class LiquidityReductionService(
    private val projectAssignmentRepository: ProjectAssignmentRepository
) {
    // 160 working hours per month approximation
    private val monthlyHours = BigDecimal("160")

    fun calculateLiquidityReductionForMonth(consultantId: Long, month: YearMonth): BigDecimal {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        val assignments = projectAssignmentRepository
            .findByConsultantIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                consultantId,
                end,
                start
            )

        var total = BigDecimal.ZERO
        assignments.filter { !it.billable }.forEach { a ->
            val allocation = BigDecimal(a.allocationPercent).divide(BigDecimal(100))
            val hours = monthlyHours.multiply(allocation)
            total = total.add(hours.multiply(a.costRate))
        }
        return total.setScale(2)
    }
}
