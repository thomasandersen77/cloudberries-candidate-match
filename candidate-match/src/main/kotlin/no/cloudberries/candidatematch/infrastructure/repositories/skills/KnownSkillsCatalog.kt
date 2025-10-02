package no.cloudberries.candidatematch.infrastructure.repositories.skills

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.atomic.AtomicReference

@Repository
class KnownSkillsCatalog(
    private val jdbcTemplate: JdbcTemplate
) {
    private val cache: AtomicReference<Set<String>> = AtomicReference(emptySet())

    fun allUppercased(): Set<String> {
        val current = cache.get()
        if (current.isNotEmpty()) return current
        val loaded = loadAll()
        cache.set(loaded)
        return loaded
    }

    fun containsUpper(nameUpper: String): Boolean = allUppercased().contains(nameUpper)

    fun normalizeAndFilter(candidates: Collection<String>): Set<String> {
        if (candidates.isEmpty()) return emptySet()
        val upper = candidates.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()
        val catalog = allUppercased()
        return upper.filter { catalog.contains(it) }.toSet()
    }

    private fun loadAll(): Set<String> {
        val sql = "SELECT DISTINCT UPPER(TRIM(name)) AS name FROM cv_skill_in_category WHERE TRIM(name) IS NOT NULL AND TRIM(name) <> ''"
        return jdbcTemplate.query(sql) { rs, _ -> rs.getString("name").trim().uppercase() }.toSet()
    }
}