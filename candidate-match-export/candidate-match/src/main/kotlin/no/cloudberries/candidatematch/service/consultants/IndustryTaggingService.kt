package no.cloudberries.candidatematch.service.consultants

import mu.KotlinLogging
import no.cloudberries.candidatematch.config.SearchLexicon
import no.cloudberries.candidatematch.infrastructure.entities.industry.CvProjectExperienceIndustryEntity
import no.cloudberries.candidatematch.infrastructure.entities.industry.IndustryEntity
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvProjectExperienceRepository
import no.cloudberries.candidatematch.infrastructure.repositories.industry.CvProjectExperienceIndustryRepository
import no.cloudberries.candidatematch.infrastructure.repositories.industry.IndustryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IndustryTaggingService(
    private val cvProjectExperienceRepository: CvProjectExperienceRepository,
    private val industryRepository: IndustryRepository,
    private val cpeiRepository: CvProjectExperienceIndustryRepository,
    private val lexicon: SearchLexicon
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun tagIndustriesForCv(cvId: Long) {
        val projects = cvProjectExperienceRepository.findByCvIdIn(listOf(cvId))
        if (projects.isEmpty()) return
        val peIds = projects.mapNotNull { it.id }
        if (peIds.isEmpty()) return
        // clear existing tags for this CV
        cpeiRepository.deleteByProjectExperienceIdIn(peIds)

        val toInsert = mutableListOf<CvProjectExperienceIndustryEntity>()
        projects.forEach { pe ->
            val text = listOfNotNull(pe.customer, pe.description, pe.longDescription).joinToString(" \n ")
            val industries = lexicon.detectIndustries(text)
            industries.forEach { canonical ->
                val industry = industryRepository.findByNameIgnoreCase(canonical) ?: industryRepository.save(IndustryEntity(name = canonical))
                toInsert.add(
                    CvProjectExperienceIndustryEntity(
                        projectExperienceId = pe.id!!,
                        industryId = industry.id!!
                    )
                )
            }
        }
        if (toInsert.isNotEmpty()) {
            cpeiRepository.saveAll(toInsert)
            logger.info { "Tagged ${toInsert.size} industry links for CV $cvId" }
        }
    }
}
