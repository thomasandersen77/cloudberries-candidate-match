package no.cloudberries.candidatematch.infrastructure.repositories

import no.cloudberries.candidatematch.infrastructure.entities.ProjectRequestEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConsultantRepository: JpaRepository<ProjectRequestEntity, Long>