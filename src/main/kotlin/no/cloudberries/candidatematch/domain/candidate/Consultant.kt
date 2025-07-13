package no.cloudberries.candidatematch.domain.candidate

data class Consultant(
    override val name: String,
    override val userId: String,
    override val cvId: String,
    val skills: Set<Skill> = emptySet()
) : Candidate {

    fun addSkill(skill: Skill): Consultant {
        return copy(skills = skills + skill)
    }

    fun addSkills(newSkills: Collection<Skill>): Consultant {
        return copy(skills = skills + newSkills)
    }

    init {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(cvId.isNotBlank()) { "CV ID cannot be blank" }
    }
}