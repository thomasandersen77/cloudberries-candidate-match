package no.cloudberries.candidatematch.infrastructure.repositories

import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import jakarta.persistence.QueryHint

@Repository
interface ConsultantRepository : JpaRepository<ConsultantEntity, Long> {
    fun findByUserId(userId: String): ConsultantEntity?
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<ConsultantEntity>
    
    @Query("SELECT DISTINCT c FROM ConsultantEntity c LEFT JOIN FETCH c.skills")
    @QueryHints(value = [
        QueryHint(name = "hibernate.query.passDistinctThrough", value = "false")
    ])
    fun findAllWithSkills(): List<ConsultantEntity>
}
