package no.cloudberries.candidatematch.domain.customer

import no.cloudberries.candidatematch.domain.candidate.Skill
import no.cloudberries.candidatematch.domain.event.DomainEvent
import no.cloudberries.candidatematch.domain.event.DomainEventPublisher

class Customer(
    val name: String,
    val cvId: String,
    val resumeData: String,
    val skills: List<Skill> = emptyList(),
    val publisher: DomainEventPublisher
) {
    val projects: MutableList<Project> = mutableListOf()

    fun addProject(project: Project) {
        if (!projects.any { it.id == project.id }) {
            projects.add(project)
        } else {
            publisher.publish(ProjectExistsEvent(project.id))
        }
    }


}