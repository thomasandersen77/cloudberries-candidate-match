package no.cloudberries.candidatematch.infrastructure.repositories.consultant

import no.cloudberries.candidatematch.infrastructure.entities.consultant.ConsultantCvEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface ConsultantCvRepository : JpaRepository<ConsultantCvEntity, Long>, JpaSpecificationExecutor<ConsultantCvEntity> {
    fun findByConsultantIdAndActiveTrue(consultantId: Long): ConsultantCvEntity?
    fun findByConsultantId(consultantId: Long): List<ConsultantCvEntity>
    fun findByConsultantId(consultantId: Long, pageable: Pageable): Page<ConsultantCvEntity>
}

