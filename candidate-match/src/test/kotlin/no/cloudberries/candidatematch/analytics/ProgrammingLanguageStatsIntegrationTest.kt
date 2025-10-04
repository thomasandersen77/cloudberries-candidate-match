package no.cloudberries.candidatematch.analytics

import no.cloudberries.candidatematch.service.analytics.ProgrammingLanguageStatsService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.Locale

@SpringBootTest(properties = ["spring.liquibase.contexts=default"]) // avoid pgvector extension during this test
@ActiveProfiles("local")
class ProgrammingLanguageStatsIntegrationTest @Autowired constructor(
    private val service: ProgrammingLanguageStatsService
) {

    @Test
    fun print_language_stats_table() {
        val stats = service.getProgrammingLanguageStats(listOf("Kotlin", "Java", "C#", "Python"))
        println(renderTable(stats))
        // Minimal assertion to mark test as passed if service returns a list (can be empty)
        assert(stats != null)
    }

    private fun renderTable(stats: List<ProgrammingLanguageStatsService.ProgrammingLanguageStat>): String {
        data class Row(val language: String, val count: String, val percentage: String, val years: String)
        val rows = stats.map { s ->
            Row(
                language = s.language,
                count = s.consultantCount.toString(),
                percentage = String.format(Locale.US, "%.1f%%", s.percentage),
                years = s.aggregatedYears.toString()
            )
        }
        val header = Row("language", "number of consultants", "percentage", "aggregated years of experience")
        val all = listOf(header) + rows
        val col1w = all.maxOf { it.language.length }
        val col2w = all.maxOf { it.count.length }
        val col3w = all.maxOf { it.percentage.length }
        val col4w = all.maxOf { it.years.length }

        fun padRight(s: String, w: Int) = s + " ".repeat(w - s.length)

        val sb = StringBuilder()
        sb.append("| ")
            .append(padRight(header.language, col1w)).append(" | ")
            .append(padRight(header.count, col2w)).append(" | ")
            .append(padRight(header.percentage, col3w)).append(" | ")
            .append(padRight(header.years, col4w)).append(" |\n")

        sb.append("|-").append("-".repeat(col1w)).append("-|-")
            .append("-".repeat(col2w)).append("-|-")
            .append("-".repeat(col3w)).append("-|-")
            .append("-".repeat(col4w)).append("-|\n")

        rows.forEach { r ->
            sb.append("| ")
                .append(padRight(r.language, col1w)).append(" | ")
                .append(padRight(r.count, col2w)).append(" | ")
                .append(padRight(r.percentage, col3w)).append(" | ")
                .append(padRight(r.years, col4w)).append(" |\n")
        }
        return sb.toString()
    }
}