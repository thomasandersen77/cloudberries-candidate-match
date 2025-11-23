package no.cloudberries.candidatematch.infrastructure.adapters

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.consultant.Consultant
import no.cloudberries.candidatematch.domain.consultant.Cv
import no.cloudberries.candidatematch.domain.consultant.PersonalInfo
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import java.time.Year
import no.cloudberries.candidatematch.domain.candidate.Skill as CandidateSkill

private val logger = KotlinLogging.logger { }

private fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

fun Consultant.toEntity(mapper: ObjectMapper = objectMapper()): ConsultantEntity {
    val resumeJson: JsonNode = mapper.readTree(this.cvAsJson)

    // Collect skills from category list and project experiences; normalize to lowercase trimmed
    val rawSkillNames: MutableSet<String> = mutableSetOf()
    // Category skills from Consultant.skills
    this.skills.forEach { s ->
        val raw = s.name.trim()
        if (raw.isNotBlank()) rawSkillNames.add(raw)
    }
    // Project experience skills
    this.cv.projectExperiences.forEach { pe ->
        pe.skillsUsed.forEach { s ->
            val raw = s.name.trim()
            if (raw.isNotBlank()) rawSkillNames.add(raw)
        }
    }

    val normalizedMapped: MutableSet<String> = mutableSetOf()
    val notInEnum: MutableSet<String> = mutableSetOf()
    rawSkillNames.forEach { raw ->
        val norm = raw.lowercase()
        normalizedMapped.add(norm)
        // Track which of the normalized names would not match a canonical skill pattern; still store them
        notInEnum.add(raw)
    }
    if (notInEnum.isNotEmpty()) {
        logger.info { "Consultant ${this.id}: ${notInEnum.size} skill(s) to be stored: ${notInEnum.joinToString(", ")}" }
    }

    return ConsultantEntity(
        id = null, // let DB assign
        name = this.personalInfo.name,
        userId = this.id,
        cvId = this.defaultCvId,
        resumeData = resumeJson
    )
}

fun ConsultantEntity.toDomain(mapper: ObjectMapper = objectMapper()): Consultant {
    val cvJson = resumeData.toString()
    val personal = PersonalInfo(
        name = this.name,
        email = this.resumeData.get("email")?.asText() ?: "unknown@example.com",
        birthYear = this.resumeData.get("bornYear")?.asInt()?.let { Year.of(it) }
    )
    val cv = Cv(id = this.cvId)
    // Skills are now handled by separate service and not directly on ConsultantEntity
    val skills = emptyList<no.cloudberries.candidatematch.domain.consultant.Skill>()

    return Consultant.builder(
        id = this.userId,
        defaultCvId = this.cvId
    )
        .withPersonalInfo(personal)
        .withCv(cv)
        .withCvAsJson(cvJson)
        .withSkills(skills)
        .build()
}