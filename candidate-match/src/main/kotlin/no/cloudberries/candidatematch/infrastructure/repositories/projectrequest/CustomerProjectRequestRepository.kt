package no.cloudberries.candidatematch.infrastructure.repositories.projectrequest

import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.CustomerProjectRequestEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomerProjectRequestRepository : JpaRepository<CustomerProjectRequestEntity, Long> {
    @EntityGraph(attributePaths = ["requirements"])
    fun findWithRequirementsById(id: Long): CustomerProjectRequestEntity?

    @EntityGraph(attributePaths = ["requirements"])
    fun findAllBy(): List<CustomerProjectRequestEntity>
}
