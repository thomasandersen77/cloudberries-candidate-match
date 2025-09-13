package no.cloudberries.candidatematch.infrastructure.repositories.consultant

import no.cloudberries.candidatematch.infrastructure.entities.consultant.ConsultantCvEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConsultantCvRepository : JpaRepository<ConsultantCvEntity, Long> {
    fun findByConsultantIdAndActiveTrue(consultantId: Long): List<ConsultantCvEntity>
}
