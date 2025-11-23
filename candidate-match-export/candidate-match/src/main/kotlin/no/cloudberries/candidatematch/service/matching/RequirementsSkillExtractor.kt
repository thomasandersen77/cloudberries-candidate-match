package no.cloudberries.candidatematch.service.matching

import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.CustomerProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.ProjectRequestRequirementEntity
import no.cloudberries.candidatematch.infrastructure.repositories.skills.KnownSkillsCatalog
import org.springframework.stereotype.Component

@Component
class RequirementsSkillExtractor(
    private val knownSkills: KnownSkillsCatalog
) {
    fun extractSkillsAny(request: CustomerProjectRequestEntity): Set<String> {
        val texts = mutableListOf<String>()
        request.title?.let { texts += it }
        request.summary?.let { texts += it }
        texts += request.requirements.mapNotNull { it.name }
        texts += request.requirements.mapNotNull { it.details }
        if (texts.isEmpty()) return emptySet()

        val tokens = texts.flatMap { tokenize(it) }
        // include bigrams
        val bigrams = buildBigrams(tokens)
        val candidates = tokens + bigrams
        return knownSkills.normalizeAndFilter(candidates)
    }

    private fun tokenize(text: String): List<String> {
        // Split on non-alphanumeric (keep letters/digits); allow hyphen to stay (SPRING-BOOT)
        val sanitized = text.replace("[^-A-Za-z0-9 ]".toRegex(), " ")
        return sanitized.split(" ")
            .map { it.trim() }
            .filter { it.length >= 2 }
    }

    private fun buildBigrams(tokens: List<String>): List<String> {
        if (tokens.size < 2) return emptyList()
        val out = ArrayList<String>(tokens.size)
        for (i in 0 until tokens.size - 1) {
            val a = tokens[i]
            val b = tokens[i + 1]
            if (a.isNotBlank() && b.isNotBlank()) {
                out.add("$a $b")
            }
        }
        return out
    }
}