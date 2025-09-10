package no.cloudberries.candidatematch.service.consultants

import no.cloudberries.candidatematch.controllers.consultants.dto.AssignmentDto
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.entities.consultant.ProjectAssignmentEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ProjectAssignmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConsultantAssignmentService(
    private val consultantRepository: ConsultantRepository,
    private val projectAssignmentRepository: ProjectAssignmentRepository
) {
    fun list(consultantId: Long): List<ProjectAssignmentEntity> =
        projectAssignmentRepository.findByConsultantId(consultantId)

    @Transactional
    fun create(consultantId: Long, dto: AssignmentDto): ProjectAssignmentEntity {
        val consultant: ConsultantEntity = consultantRepository.findById(consultantId).orElseThrow()
        val entity = ProjectAssignmentEntity(
            id = null,
            consultant = consultant,
            title = dto.title,
            startDate = dto.startDate,
            endDate = dto.endDate,
            allocationPercent = dto.allocationPercent,
            hourlyRate = dto.hourlyRate,
            costRate = dto.costRate,
            clientProjectRef = dto.clientProjectRef,
            billable = dto.billable,
        )
        return projectAssignmentRepository.save(entity)
    }

    @Transactional
    fun update(consultantId: Long, assignmentId: Long, dto: AssignmentDto): ProjectAssignmentEntity {
        val existing = projectAssignmentRepository.findById(assignmentId).orElseThrow()
        require(existing.consultant.id == consultantId) { "Assignment does not belong to consultant" }
        val updated = ProjectAssignmentEntity(
            id = existing.id,
            consultant = existing.consultant,
            title = dto.title,
            startDate = dto.startDate,
            endDate = dto.endDate,
            allocationPercent = dto.allocationPercent,
            hourlyRate = dto.hourlyRate,
            costRate = dto.costRate,
            clientProjectRef = dto.clientProjectRef,
            billable = dto.billable,
            version = existing.version
        )
        return projectAssignmentRepository.save(updated)
    }

    @Transactional
    fun delete(consultantId: Long, assignmentId: Long) {
        val existing = projectAssignmentRepository.findById(assignmentId).orElseThrow()
        require(existing.consultant.id == consultantId) { "Assignment does not belong to consultant" }
        projectAssignmentRepository.delete(existing)
    }
}

