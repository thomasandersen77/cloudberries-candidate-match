package no.cloudberries.candidatematch.service.projectrequest.parser

import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.RequirementPriority
import org.springframework.stereotype.Component

@Component
class DefaultRequirementParser : RequirementParser {
    private val markerRegex = Regex("(?i)(MUST|SHOULD)\\s*:\\s*")

    override fun parse(text: String): List<ParsedRequirement> {
        if (text.isBlank()) return emptyList()
        val matches = markerRegex.findAll(text).toList()
        if (matches.isNotEmpty()) return parseWithMarkers(text, matches)
        return parseFallback(text)
    }

    private fun parseWithMarkers(text: String, matches: List<MatchResult>): List<ParsedRequirement> {
        val out = mutableListOf<ParsedRequirement>()
        for (i in matches.indices) {
            val m = matches[i]
            val priority = if (m.groupValues[1].equals("MUST", ignoreCase = true)) RequirementPriority.MUST else RequirementPriority.SHOULD
            val start = m.range.last + 1
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            val segment = text.substring(start, end).trim()
            if (segment.isBlank()) continue
            val parts = segment
                .split(Regex("[\\n;\\u2022•]+"))
                .map { it.trim().trim('-').trim('*').trim() }
                .filter { it.isNotBlank() }
            val items = if (parts.isEmpty()) listOf(segment) else parts
            items.forEach { item ->
                val name = item.take(500)
                if (name.isNotBlank()) out += ParsedRequirement(name = name, details = null, priority = priority)
            }
        }
        return out.take(200)
    }

    private fun parseFallback(text: String): List<ParsedRequirement> {
        return text
            .replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val normalized = line.lowercase()
                val priority = if (normalized.contains("should")) RequirementPriority.SHOULD else RequirementPriority.MUST
                val name = line.removePrefix("-").removePrefix("*").trim().take(500)
                if (name.isBlank()) null else ParsedRequirement(name = name, details = null, priority = priority)
            }
            .take(200)
    }
}