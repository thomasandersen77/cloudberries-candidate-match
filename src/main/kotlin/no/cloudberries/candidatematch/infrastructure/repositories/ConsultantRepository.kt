package no.cloudberries.candidatematch.infrastructure.repositories

import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface ConsultantRepository : JpaRepository<ConsultantEntity, Long>, JpaSpecificationExecutor<ConsultantEntity> {
    fun findByUserId(userId: String): ConsultantEntity?
}
