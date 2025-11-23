package no.cloudberries.candidatematch.service.consultants

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.entities.consultant.ConsultantCvEntity
import no.cloudberries.candidatematch.infrastructure.entities.scoring.CvScoreEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ConsultantCvRepository
import no.cloudberries.candidatematch.infrastructure.repositories.scoring.CvScoreRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.mockk

class CvDataAggregationServiceTest {

    private val objectMapper = ObjectMapper()

    private val consultantCvRepository = mockk<ConsultantCvRepository>()
    private val cvKeyQualificationRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvKeyQualificationRepository>()
    private val cvEducationRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvEducationRepository>()
    private val cvWorkExperienceRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvWorkExperienceRepository>()
    private val cvProjectExperienceRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvProjectExperienceRepository>()
    private val cvProjectExperienceRoleRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvProjectExperienceRoleRepository>()
    private val cvProjectExperienceSkillRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvProjectExperienceSkillRepository>()
    private val cvCertificationRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvCertificationRepository>()
    private val cvCourseRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvCourseRepository>()
    private val cvLanguageRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvLanguageRepository>()
    private val cvSkillCategoryRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvSkillCategoryRepository>()
    private val cvSkillInCategoryRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvSkillInCategoryRepository>()
    private val cvAttachmentRepository = mockk<no.cloudberries.candidatematch.infrastructure.repositories.consultant.CvAttachmentRepository>()
    private val industryRepo = mockk<no.cloudberries.candidatematch.infrastructure.repositories.industry.IndustryRepository>()
    private val cpeiRepo = mockk<no.cloudberries.candidatematch.infrastructure.repositories.industry.CvProjectExperienceIndustryRepository>()
    private val consultantRepository = mockk<ConsultantRepository>()
    private val cvScoreRepository = mockk<CvScoreRepository>()
    private val skillService = mockk<no.cloudberries.candidatematch.domain.candidate.SkillService>()

    private val service = CvDataAggregationService(
        consultantCvRepository,
        cvKeyQualificationRepository,
        cvEducationRepository,
        cvWorkExperienceRepository,
        cvProjectExperienceRepository,
        cvProjectExperienceRoleRepository,
        cvProjectExperienceSkillRepository,
        cvCertificationRepository,
        cvCourseRepository,
        cvLanguageRepository,
        cvSkillCategoryRepository,
        cvSkillInCategoryRepository,
        cvAttachmentRepository,
        industryRepo,
        cpeiRepo,
        consultantRepository,
        cvScoreRepository,
        skillService
    )

    private fun createConsultantWithCv(userId: String, name: String, activeCv: Boolean = true): Pair<ConsultantEntity, ConsultantCvEntity> {
        val resumeJson: ObjectNode = objectMapper.createObjectNode().put("cv", "cv-$userId")
        val consultant = ConsultantEntity(
            id = 1L,
            name = name,
            userId = userId,
            cvId = "cv-$userId",
            resumeData = resumeJson
        )
        val cv = ConsultantCvEntity(
            id = 10L,
            consultantId = consultant.id!!,
            versionTag = "v1",
            qualityScore = null,
            active = activeCv
        )
        every { consultantCvRepository.findByConsultantIdInAndActiveTrue(listOf(consultant.id!!)) } returns listOf(cv)
        every { consultantCvRepository.findByConsultantIdIn(listOf(consultant.id!!)) } returns listOf(cv)
        every { consultantRepository.findAllById(listOf(consultant.id!!)) } returns listOf(consultant)
        // default empty loads for related repos
        every { cvProjectExperienceRepository.findByCvIdIn(any()) } returns emptyList()
        every { cpeiRepo.findByProjectExperienceIdIn(any()) } returns emptyList()
        every { industryRepo.findAllById(any<Set<Long>>()) } returns emptyList()
        every { cvKeyQualificationRepository.findByCvIdIn(any()) } returns emptyList()
        every { cvEducationRepository.findByCvIdIn(any()) } returns emptyList()
        every { cvWorkExperienceRepository.findByCvIdIn(any()) } returns emptyList()
        every { cvProjectExperienceRoleRepository.findByProjectExperienceIdIn(any()) } returns emptyList()
        every { cvProjectExperienceSkillRepository.findByProjectExperienceIdIn(any()) } returns emptyList()
        every { cvCertificationRepository.findByCvIdIn(any()) } returns emptyList()
        every { cvCourseRepository.findByCvIdIn(any()) } returns emptyList()
        every { cvLanguageRepository.findByCvIdIn(any()) } returns emptyList()
        every { cvSkillCategoryRepository.findByCvIdIn(any()) } returns emptyList()
        every { cvSkillInCategoryRepository.findBySkillCategoryIdIn(any()) } returns emptyList()
        every { cvAttachmentRepository.findByCvIdIn(any()) } returns emptyList()
        return consultant to cv
    }

    @Test
    fun `should populate qualityScore from cv_score when present`() {
        val (consultant, _) = createConsultantWithCv("u-agg-1", "Alice", activeCv = true)
        // mock cv_score with 85
        every { cvScoreRepository.findByCandidateUserIdIn(setOf(consultant.userId)) } returns listOf(
            CvScoreEntity(
                id = 1L,
                candidateUserId = consultant.userId,
                name = consultant.name,
                scorePercent = 85,
                summary = "",
                strengths = null,
                potentialImprovements = null
            )
        )
        val result = service.aggregateCvData(listOf(consultant.id!!), onlyActiveCv = true)
        val cvs = result[consultant.id!!]
        assertThat(cvs).isNotNull()
        assertThat(cvs!!.first().qualityScore).isEqualTo(85)
    }

    @Test
    fun `should return null qualityScore when cv_score missing`() {
        val (consultant, _) = createConsultantWithCv("u-agg-2", "Bob", activeCv = true)

        every { cvScoreRepository.findByCandidateUserIdIn(setOf(consultant.userId)) } returns emptyList()

        val result = service.aggregateCvData(listOf(consultant.id!!), onlyActiveCv = true)
        val cvs = result[consultant.id!!]
        assertThat(cvs).isNotNull()
        assertThat(cvs!!.first().qualityScore).isNull()
    }
}