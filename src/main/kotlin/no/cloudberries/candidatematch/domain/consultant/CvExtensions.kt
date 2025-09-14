package no.cloudberries.candidatematch.domain.consultant

// Domain behavior extensions for Consultant aggregate

/**
 * Flattens the CV domain object to a plain-text representation suitable for
 * downstream embedding providers. This avoids leaking Flowcase DTOs outside
 * the integration layer and keeps the transformation close to the domain.
 */
fun Cv.toFlatText(): String {
    val parts = mutableListOf<String>()

    // Key qualifications
    parts += keyQualifications.flatMap {
        listOf(
            it.label,
            it.description
        )
    }

    // Project experiences
    projectExperiences.forEach { pe ->
        parts += listOf(
            pe.customer,
            pe.description,
            pe.longDescription
        )
        parts += pe.roles.flatMap { r ->
            listOf(
                r.name,
                r.description
            )
        }
        parts += pe.skillsUsed.map { s -> s.name }
    }

    // Work experiences
    workExperiences.forEach { we ->
        parts += listOfNotNull(
            we.employer,
            we.period.from?.toString(),
            we.period.to?.toString()
        )
    }

    // Technologies / skills
    skillCategories.forEach { sc ->
        parts.add(sc.name)
        parts += sc.skills.flatMap { s ->
            listOfNotNull(
                s.name,
                s.durationInYears?.toString()
            )
        }
    }

    // Education
    educations.forEach { e ->
        parts += listOfNotNull(
            e.degree,
            e.school,
            e.period.from?.toString(),
            e.period.to?.toString()
        )
    }

    // Certifications
    certifications.forEach { c ->
        parts += listOfNotNull(
            c.name,
            c.year?.toString()
        )
    }

    // Courses
    courses.forEach { c ->
        parts += listOfNotNull(
            c.name,
            c.organizer,
            c.year?.toString()
        )
    }

    // Languages
    languages.forEach { l ->
        parts += listOf(
            l.name,
            l.level
        )
    }

    return parts.filter { !it.isNullOrBlank() }.joinToString(separator = "\n")
}

/**
 * Computes a simple skill match score as percentage of required skills
 * that the consultant possesses. Values are rounded down to integer percent.
 */
fun Consultant.skillMatchScore(required: Set<no.cloudberries.candidatematch.domain.candidate.Skill>): Int {
    if (required.isEmpty()) return 0
    val consultantSkillsAsEnum: Set<no.cloudberries.candidatematch.domain.candidate.Skill> =
        this.skills.mapNotNull { s ->
            runCatching {
                no.cloudberries.candidatematch.domain.candidate.Skill.valueOf(
                    s.name.trim().uppercase()
                )
            }.getOrNull()
        }.toSet()
    val matched = consultantSkillsAsEnum.intersect(required)
    val ratio = matched.size.toDouble() / required.size.toDouble()
    return (ratio * 100).toInt()
}
