package no.cloudberries.candidatematch.domain.customer

import no.cloudberries.candidatematch.domain.event.DomainEvent
import no.cloudberries.candidatematch.domain.event.DomainEventPublisher

class Customer(
    val name: String,
    val publisher: DomainEventPublisher
) {
    val projects: MutableList<Project> = mutableListOf()

    fun addProject(project: Project) {
        projects.forEach {
            if (!it.exists(project.id)) {
                projects.add(project)
            } else {
                publisher.publish(ProjectExistsEvent(project.id))
            }
        }
    }


}