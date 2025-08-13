package no.cloudberries.candidatematch.repositories

import no.cloudberries.candidatematch.entities.ProjectRequestEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRequestRepository : JpaRepository<ProjectRequestEntity, Long> {
    // You can now add custom query methods here if needed.
    // For example, to find requests by customer name:
    // fun findByCustomerName(customerName: String): List<ProjectRequest>
}