package no.cloudberries.candidatematch.infrastructure.repositories

import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConsultantRepository : JpaRepository<ConsultantEntity, Long> {
    fun findByUserId(userId: String): ConsultantEntity?
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<ConsultantEntity>
}
