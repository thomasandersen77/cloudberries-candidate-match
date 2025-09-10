package no.cloudberries.candidatematch.infrastructure.repositories.consultant

import no.cloudberries.candidatematch.infrastructure.entities.consultant.ProjectAssignmentEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ProjectAssignmentRepository : JpaRepository<ProjectAssignmentEntity, Long>, JpaSpecificationExecutor<ProjectAssignmentEntity> {
    fun findByConsultantId(consultantId: Long): List<ProjectAssignmentEntity>
    fun findByConsultantId(consultantId: Long, pageable: Pageable): Page<ProjectAssignmentEntity>
    fun findByConsultantIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        consultantId: Long,
        start: LocalDate,
        end: LocalDate
    ): List<ProjectAssignmentEntity>
}

