package no.cloudberries.candidatematch.service.embedding

import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseCvDto

object FlowcaseCvTextFlattener {
    fun toText(cv: FlowcaseCvDto): String {
        val parts = mutableListOf<String>()
        parts += listOfNotNull(
            cv.name,
            cv.title?.text,
            cv.placeOfResidence?.text,
            cv.nationality?.text
        )
        cv.keyQualifications.filterNot { it.disabled }.forEach { kq ->
            parts += listOfNotNull(
                kq.label?.text,
                kq.longDescription?.text
            )
        }
        cv.projectExperiences.filterNot { it.disabled }.forEach { pe ->
            parts += listOfNotNull(
                pe.customer?.text,
                pe.description?.text,
                pe.longDescription?.text
            )
            pe.roles.filterNot { it.disabled }.forEach { r ->
                parts += listOfNotNull(
                    r.name?.text,
                    r.description?.text,
                    r.longDescription?.text
                )
            }
            pe.projectExperienceSkills.filterNot { it.disabled }.forEach { s ->
                parts += listOfNotNull(s.tags?.text)
            }
        }
        cv.workExperiences.filterNot { it.disabled }.forEach { we ->
            parts += listOfNotNull(
                we.employer?.text,
                we.yearFrom,
                we.yearTo,
                we.monthFrom,
                we.monthTo
            )
        }
        cv.technologies.filterNot { it.disabled }.forEach { t ->
            parts += listOfNotNull(t.category?.text)
            t.technologySkills.filterNot { it.disabled }.forEach { ts ->
                parts += listOfNotNull(
                    ts.tags?.text,
                    ts.totalDurationInYears?.toString()
                )
            }
        }
        cv.educations.filterNot { it.disabled }.forEach { e ->
            parts += listOfNotNull(
                e.degree?.text,
                e.school?.text,
                e.yearFrom,
                e.yearTo
            )
        }
        cv.courses.filterNot { it.disabled }.forEach { c ->
            parts += listOfNotNull(
                c.name?.text,
                c.program?.text,
                c.year
            )
        }
        cv.certifications.filterNot { it.disabled }.forEach { c ->
            parts += listOfNotNull(
                c.name?.text,
                c.year
            )
        }
        cv.languages.filterNot { it.disabled }.forEach { l ->
            parts += listOfNotNull(
                l.name?.text,
                l.level?.text
            )
        }
        return parts.filter { !it.isNullOrBlank() }.joinToString(separator = "\n")
    }
}