package no.cloudberries.candidatematch.service.projectrequest.parser

import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.RequirementPriority

interface RequirementParser {
    fun parse(text: String): List<ParsedRequirement>
}

data class ParsedRequirement(
    val name: String,
    val details: String? = null,
    val priority: RequirementPriority,
)