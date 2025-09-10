package no.cloudberries.candidatematch.service.consultants

import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ProjectAssignmentRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

@Service
class LiquidityReductionService(
    private val projectAssignmentRepository: ProjectAssignmentRepository
) {
    /**
     * Calculates liquidity reduction for a consultant in a given month.
     * Assumptions:
     * - Only non-billable assignments reduce liquidity (billable=false)
     * - Uses costRate per hour
     * - 8 hours per working day, approximated as 160 hours per month (adjusted proportionally for overlap days)
     */
    fun calculateLiquidityReductionForMonth(consultantId: Long, month: YearMonth): BigDecimal {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val assignments = projectAssignmentRepository
            .findByConsultantId(consultantId)
            .filter { overlaps(it.startDate, it.endDate, monthStart, monthEnd) }

        var total = BigDecimal.ZERO
        val hoursInMonth = BigDecimal(160)

        for (a in assignments) {
            if (a.billable) continue
            val overlapDays = overlapDays(a.startDate, a.endDate, monthStart, monthEnd)
            if (overlapDays <= 0) continue
            val proportion = BigDecimal(overlapDays).divide(BigDecimal(month.lengthOfMonth()), 6, RoundingMode.HALF_UP)
            val hours = hoursInMonth.multiply(proportion)
            val allocationFactor = BigDecimal(a.allocationPercent).divide(BigDecimal(100), 6, RoundingMode.HALF_UP)
            val costRate = a.costRate ?: BigDecimal.ZERO
            val reduction = costRate.multiply(hours).multiply(allocationFactor)
            total = total.add(reduction)
        }
        return total.setScale(2, RoundingMode.HALF_UP)
    }

    private fun overlaps(aStart: LocalDate, aEnd: LocalDate?, bStart: LocalDate, bEnd: LocalDate): Boolean {
        val end = aEnd ?: LocalDate.MAX
        return !aStart.isAfter(bEnd) && !end.isBefore(bStart)
    }

    private fun overlapDays(aStart: LocalDate, aEnd: LocalDate?, bStart: LocalDate, bEnd: LocalDate): Int {
        val start = maxOf(aStart, bStart)
        val end = minOf(aEnd ?: LocalDate.MAX, bEnd)
        return if (start.isAfter(end)) 0 else (end.toEpochDay() - start.toEpochDay() + 1).toInt()
    }
}

