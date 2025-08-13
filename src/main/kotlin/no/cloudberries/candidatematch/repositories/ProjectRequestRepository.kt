package no.cloudberries.candidatematch.repositories

import no.cloudberries.candidatematch.entities.ProjectRequestEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ProjectRequestRepository : JpaRepository<ProjectRequestEntity, Long> {
    // You can now add custom query methods here if needed.
    // For example, to find requests by customer name:
    // fun findByCustomerName(customerName: String): List<ProjectRequest>
    // Inne i ProjectRequestRepository-interfacet

    /**
     * Retrieves a list of open project requests with a response deadline between the given time range.
     *
     * @param from the start of the response deadline period to search for open project requests.
     * @param to the end of the response deadline period to search for open project requests.
     * @return a list of ProjectRequestEntity that are open and have a response deadline within the specified period.
     */
    @Query(
        "SELECT pr FROM ProjectRequestEntity pr " +
                "WHERE pr.status = 'OPEN' " +
                "AND pr.responseDeadline BETWEEN :from AND :to"
    )
    fun findOpenRequestsWithDeadlineBetween(from: LocalDateTime, to: LocalDateTime): List<ProjectRequestEntity>
}