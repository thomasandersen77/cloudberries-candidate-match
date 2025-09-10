package no.cloudberries.candidatematch.service.consultants

import jakarta.transaction.Transactional
import no.cloudberries.candidatematch.controllers.consultants.ConsultantDtoMapper
import no.cloudberries.candidatematch.controllers.consultants.dto.CvDto
import no.cloudberries.candidatematch.infrastructure.entities.consultant.ConsultantCvEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ConsultantCvRepository
import org.springframework.stereotype.Service

@Service
class ConsultantCvService(
    private val consultantRepository: ConsultantRepository,
    private val consultantCvRepository: ConsultantCvRepository,
) {
    fun listCvs(consultantId: Long): List<ConsultantCvEntity> {
        return consultantCvRepository.findByConsultantId(consultantId)
    }

    @Transactional
    fun createCv(consultantId: Long, dto: CvDto): ConsultantCvEntity {
        val consultant = consultantRepository.findById(consultantId).orElseThrow()
        val entity = ConsultantDtoMapper.fromDto(consultant, dto.copy(id = null))
        return consultantCvRepository.save(entity)
    }

    @Transactional
    fun activate(consultantId: Long, cvId: Long) {
        // Deactivate current active
        consultantCvRepository.findByConsultantIdAndActiveTrue(consultantId)?.let { active ->
            if (active.id != cvId) {
                val updated = ConsultantCvEntity(
                    id = active.id,
                    consultant = active.consultant,
                    versionTag = active.versionTag,
                    active = false,
                    qualityScore = active.qualityScore,
                    version = active.version
                )
                // Re-attach children and save
                updated.keyQualifications.addAll(active.keyQualifications)
                updated.educations.addAll(active.educations)
                updated.workExperiences.addAll(active.workExperiences)
                updated.projectExperiences.addAll(active.projectExperiences)
                updated.certifications.addAll(active.certifications)
                updated.courses.addAll(active.courses)
                updated.languages.addAll(active.languages)
                updated.skillCategories.addAll(active.skillCategories)
                updated.attachments.addAll(active.attachments)
                consultantCvRepository.save(updated)
            }
        }
        // Activate chosen
        val toActivate = consultantCvRepository.findById(cvId).orElseThrow()
        val activated = ConsultantCvEntity(
            id = toActivate.id,
            consultant = toActivate.consultant,
            versionTag = toActivate.versionTag,
            active = true,
            qualityScore = toActivate.qualityScore,
            version = toActivate.version
        )
        activated.keyQualifications.addAll(toActivate.keyQualifications)
        activated.educations.addAll(toActivate.educations)
        activated.workExperiences.addAll(toActivate.workExperiences)
        activated.projectExperiences.addAll(toActivate.projectExperiences)
        activated.certifications.addAll(toActivate.certifications)
        activated.courses.addAll(toActivate.courses)
        activated.languages.addAll(toActivate.languages)
        activated.skillCategories.addAll(toActivate.skillCategories)
        activated.attachments.addAll(toActivate.attachments)
        consultantCvRepository.save(activated)
    }
}

