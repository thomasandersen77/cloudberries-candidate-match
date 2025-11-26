package no.cloudberries.candidatematch.service.cv

import no.cloudberries.candidatematch.domain.consultant.Consultant
import org.springframework.stereotype.Service

/**
 * Converts CV data to Markdown format optimized for Gemini Files API.
 * 
 * Markdown provides better structure than plain text and is more token-efficient
 * than JSON for long-form content. This format helps Gemini understand the CV
 * structure and content more effectively.
 */
@Service
class CvToMarkdownConverter {

    /**
     * Converts a consultant's CV to well-formatted Markdown.
     * 
     * @param consultant The consultant aggregate containing all CV data
     * @return Markdown formatted CV as String
     */
    fun convert(consultant: Consultant): String {
        return buildString {
            // Header with consultant name
            appendLine("# ${consultant.personalInfo.name}")
            appendLine()

            // Basic Information
            appendLine("## Informasjon")
            appendLine("- **ID**: ${consultant.id}")
            appendLine("- **E-post**: ${consultant.personalInfo.email}")
            consultant.personalInfo.birthYear?.let {
                appendLine("- **Fødselsår**: $it")
            }
            appendLine()

            // Skills
            appendLine("## Ferdigheter")
            if (consultant.skills.isNotEmpty()) {
                consultant.skills
                    .sortedByDescending { it.durationInYears ?: 0 }
                    .forEach { skill ->
                        val years = skill.durationInYears?.let { " ($it år)" } ?: ""
                        appendLine("- **${skill.name}**$years")
                    }
            } else {
                appendLine("Ingen ferdigheter registrert.")
            }
            appendLine()

            // CV Quality Score
            consultant.cv.qualityScore?.let { score ->
                appendLine("## CV Kvalitet")
                appendLine("- **Score**: $score/100")
                appendLine()
            }

            // Key Qualifications
            if (consultant.cv.keyQualifications.isNotEmpty()) {
                appendLine("## Nøkkelkvalifikasjoner")
                consultant.cv.keyQualifications.forEach { qual ->
                    appendLine("### ${qual.label}")
                    appendLine(qual.description)
                    appendLine()
                }
            }

            // Work Experience
            appendLine("## Arbeidserfaring")
            if (consultant.cv.workExperiences.isNotEmpty()) {
                consultant.cv.workExperiences.forEach { work ->
                    val period = formatPeriod(work.period.from?.toString(), work.period.to?.toString())
                    appendLine("### ${work.employer}")
                    appendLine("*$period*")
                    appendLine()
                }
            } else {
                appendLine("Ingen arbeidserfaring registrert.")
            }
            appendLine()

            // Project Experience
            if (consultant.cv.projectExperiences.isNotEmpty()) {
                appendLine("## Prosjekterfaring")
                consultant.cv.projectExperiences.forEach { project ->
                    val period = formatPeriod(project.period.from?.toString(), project.period.to?.toString())
                    appendLine("### ${project.customer}")
                    appendLine("*$period*")
                    appendLine()
                    appendLine("**Beskrivelse**: ${project.description}")
                    if (project.longDescription.isNotBlank()) {
                        appendLine()
                        appendLine(project.longDescription)
                    }
                    if (project.roles.isNotEmpty()) {
                        appendLine()
                        appendLine("**Roller**:")
                        project.roles.forEach { role ->
                            appendLine("- ${role.name}: ${role.description}")
                        }
                    }
                    if (project.skillsUsed.isNotEmpty()) {
                        appendLine()
                        appendLine("**Teknologier**: ${project.skillsUsed.joinToString(", ") { it.name }}")
                    }
                    appendLine()
                }
            }

            // Education
            appendLine("## Utdanning")
            if (consultant.cv.educations.isNotEmpty()) {
                consultant.cv.educations.forEach { edu ->
                    val period = formatPeriod(edu.period.from?.toString(), edu.period.to?.toString())
                    appendLine("### ${edu.degree} - ${edu.school}")
                    appendLine("*$period*")
                    appendLine()
                }
            } else {
                appendLine("Ingen utdanning registrert.")
            }
            appendLine()

            // Certifications
            if (consultant.cv.certifications.isNotEmpty()) {
                appendLine("## Sertifiseringer")
                consultant.cv.certifications.forEach { cert ->
                    val year = cert.year?.toString() ?: "Ukjent år"
                    appendLine("- ${cert.name} ($year)")
                }
                appendLine()
            }

            // Courses
            if (consultant.cv.courses.isNotEmpty()) {
                appendLine("## Kurs")
                consultant.cv.courses.forEach { course ->
                    val year = course.year?.toString() ?: "Ukjent år"
                    appendLine("- ${course.name} - ${course.organizer} ($year)")
                }
                appendLine()
            }

            // Languages
            if (consultant.cv.languages.isNotEmpty()) {
                appendLine("## Språk")
                consultant.cv.languages.forEach { lang ->
                    appendLine("- ${lang.name} - ${lang.level}")
                }
                appendLine()
            }

            // Skill Categories (alternative representation)
            if (consultant.cv.skillCategories.isNotEmpty()) {
                appendLine("## Kompetanseområder")
                consultant.cv.skillCategories.forEach { category ->
                    appendLine("### ${category.name}")
                    category.skills.forEach { skill ->
                        val years = skill.durationInYears?.let { " ($it år)" } ?: ""
                        appendLine("- ${skill.name}$years")
                    }
                    appendLine()
                }
            }
        }
    }

    private fun formatPeriod(from: String?, to: String?): String {
        return when {
            from != null && to != null -> "$from - $to"
            from != null -> "$from - Nå"
            else -> "Ukjent periode"
        }
    }
}
