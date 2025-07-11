package no.cloudberries.candidatematch.repositories

import no.cloudberries.candidatematch.domain.ProjectRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConsultantRepository: JpaRepository<ProjectRequest, Long> {
    fun findByConsultantName(consultantName: String): List<ProjectRequest>
}