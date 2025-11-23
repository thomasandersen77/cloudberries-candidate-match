package no.cloudberries.candidatematch.service.matching

import no.cloudberries.candidatematch.controllers.consultants.ConsultantCvDto
import no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto

object ConsultantCvTextFlattener {
    fun toText(consultant: ConsultantWithCvDto, maxChars: Int = 8000): String {
        val parts = mutableListOf<String>()
        parts += listOf(
            consultant.name,
            consultant.skills.joinToString(", ")
        )
        consultant.cvs.forEach { cv ->
            parts += flattenCv(cv)
        }
        val text = parts.filter { it.isNotBlank() }.joinToString("\n")
        return if (text.length > maxChars) text.substring(0, maxChars) else text
    }

    private fun flattenCv(cv: ConsultantCvDto): List<String> {
        val out = mutableListOf<String>()
        // Key qualifications
        cv.keyQualifications.forEach { kq ->
            kq.label?.let { out += it }
            kq.description?.let { out += it }
        }
        // Project experience - latest first as given
        cv.projectExperience.take(10).forEach { pe ->
            pe.customer?.let { out += it }
            pe.description?.let { out += it }
            pe.longDescription?.let { out += it }
            if (pe.skills.isNotEmpty()) out += pe.skills.joinToString(", ")
        }
        // Certifications
        cv.certifications.forEach { c -> c.name?.let { out += it } }
        // Education
        cv.education.forEach { e ->
            e.degree?.let { out += it }
            e.school?.let { out += it }
        }
        // Languages
        cv.languages.forEach { l -> l.name?.let { out += it } }
        return out
    }
}