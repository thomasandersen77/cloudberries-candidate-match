package no.cloudberries.candidatematch.domain.candidate

class Consultant(
    val name: String,
) {
    val skills: MutableList<Skill> = mutableListOf<Skill>()

    fun addSkill(skill: Skill) {
        skills.add(skill)
    }

}