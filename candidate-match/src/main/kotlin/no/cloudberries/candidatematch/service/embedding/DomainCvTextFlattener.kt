package no.cloudberries.candidatematch.service.embedding

import no.cloudberries.candidatematch.domain.consultant.Cv

object DomainCvTextFlattener {
    fun toText(cv: Cv, maxChars: Int = 8000): String {
        val parts = mutableListOf<String>()
        // Key qualifications
        cv.keyQualifications.forEach { kq ->
            if (kq.label.isNotBlank()) parts += kq.label
            if (kq.description.isNotBlank()) parts += kq.description
        }
        // Skill categories
        cv.skillCategories.forEach { cat ->
            if (cat.name.isNotBlank()) parts += cat.name
            val skillLine = cat.skills.mapNotNull { it.name.ifBlank { null } }.joinToString(", ")
            if (skillLine.isNotBlank()) parts += skillLine
        }
        // Project experiences (limit to recent items)
        cv.projectExperiences.take(10).forEach { pe ->
            if (pe.customer.isNotBlank()) parts += pe.customer
            if (pe.description.isNotBlank()) parts += pe.description
            if (pe.longDescription.isNotBlank()) parts += pe.longDescription
            val used = pe.skillsUsed.mapNotNull { it.name.ifBlank { null } }.joinToString(", ")
            if (used.isNotBlank()) parts += used
        }
        // Work experiences
        cv.workExperiences.forEach { we ->
            if (we.employer.isNotBlank()) parts += we.employer
        }
        // Certifications
        cv.certifications.forEach { c -> if (c.name.isNotBlank()) parts += c.name }
        // Courses
        cv.courses.forEach { c -> if (c.name.isNotBlank()) parts += c.name }
        // Languages
        cv.languages.forEach { l ->
            if (l.name.isNotBlank()) parts += l.name
            if (l.level.isNotBlank()) parts += l.level
        }
        val text = parts.filter { it.isNotBlank() }.joinToString("\n")
        return if (text.length > maxChars) text.substring(0, maxChars) else text
    }
}