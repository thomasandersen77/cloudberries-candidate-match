package no.cloudberries.candidatematch.service.analytics

import no.cloudberries.candidatematch.infrastructure.repositories.analytics.ProgrammingLanguageStatsRepository
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class ProgrammingLanguageStatsService(
    private val repo: ProgrammingLanguageStatsRepository
) {
    data class ProgrammingLanguageStat(
        val language: String,
        val consultantCount: Int,
        val percentage: Double,
        val aggregatedYears: Int
    )

    private val canonicalToSynonymsUpper: Map<String, Set<String>> = mapOf(
        "Kotlin" to setOf("KOTLIN"),
        "Java" to setOf("JAVA"),
        "C#" to setOf("C#", "C SHARP", "C-SHARP", "CSHARP", "C SHARP (C#)"),
        "Python" to setOf("PYTHON")
    )

    fun getProgrammingLanguageStats(targetLanguages: List<String>? = null): List<ProgrammingLanguageStat> {
        val canonicalTargets = (targetLanguages ?: listOf("Kotlin", "Java", "C#", "Python"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        // Gather all synonyms in UPPERCASE for filtering
        val upperSynonyms = canonicalTargets.flatMap { lang ->
            canonicalToSynonymsUpper[lang] ?: setOf(lang.uppercase(Locale.getDefault()))
        }.map { it.uppercase(Locale.getDefault()) }.toSet()

        val totalConsultants = repo.countTotalConsultants().coerceAtLeast(1) // avoid div-by-zero

        // Fetch rows from all sources (normalized + string-based)
        val rows = mutableListOf<ProgrammingLanguageStatsRepository.ConsultantSkillRow>()
        rows += repo.fetchNormalizedConsultantSkills(upperSynonyms)
        rows += repo.fetchFromCvSkillInCategory(upperSynonyms)
        rows += repo.fetchFromCvProjectExperienceSkill(upperSynonyms)

        // Build canonical -> consultantId -> aggregatedYears map
        data class Acc(var years: Int)
        val byCanonical: MutableMap<String, MutableMap<Long, Acc>> = mutableMapOf()

        fun canonicalOf(rawUpper: String): String? =
            canonicalToSynonymsUpper.entries.firstOrNull { (_, syns) -> rawUpper in syns }?.key
                ?: canonicalTargets.firstOrNull { it.equals(rawUpper, ignoreCase = true) }

        rows.forEach { r ->
            val canonical = canonicalOf(r.rawSkillUpper) ?: return@forEach
            val perLang = byCanonical.getOrPut(canonical) { mutableMapOf() }
            val acc = perLang.getOrPut(r.consultantId) { Acc(0) }
            // Sum duration years across occurrences for the same consultant & language
            acc.years += r.durationYears.coerceAtLeast(0)
        }

        val stats = canonicalTargets.map { canonical ->
            val perLang = byCanonical[canonical] ?: emptyMap<Long, Acc>()
            val consultantCount = perLang.size
            val aggregatedYears = perLang.values.sumOf { it.years }
            ProgrammingLanguageStat(
                language = canonical,
                consultantCount = consultantCount,
                percentage = (consultantCount * 100.0) / totalConsultants.toDouble(),
                aggregatedYears = aggregatedYears
            )
        }.sortedWith(
            compareByDescending<ProgrammingLanguageStat> { it.consultantCount }
                .thenByDescending { it.aggregatedYears }
                .thenBy { it.language.lowercase(Locale.getDefault()) }
        )

        return stats
    }
}
