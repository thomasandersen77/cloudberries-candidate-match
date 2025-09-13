package no.cloudberries.candidatematch.infrastructure.adapters

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.cloudberries.candidatematch.domain.consultant.Consultant
import no.cloudberries.candidatematch.domain.consultant.Cv
import no.cloudberries.candidatematch.domain.consultant.PersonalInfo
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import java.time.Year
import no.cloudberries.candidatematch.domain.candidate.Skill as CandidateSkill

private fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

fun Consultant.toEntity(mapper: ObjectMapper = objectMapper()): ConsultantEntity {
    val resumeJson: JsonNode = mapper.readTree(this.cvAsJson)
    val mappedSkills: MutableSet<CandidateSkill> = this.skills
        .mapNotNull { s -> runCatching { CandidateSkill.valueOf(s.name.uppercase()) }.getOrNull() }
        .toMutableSet()

    return ConsultantEntity(
        id = null, // let DB assign
        name = this.personalInfo.name,
        userId = this.id,
        cvId = this.defaultCvId,
        resumeData = resumeJson,
        skills = mappedSkills,
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
    val skills = this.skills.map { s ->
        no.cloudberries.candidatematch.domain.consultant.Skill(
            s.name,
            null
        )
    }

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