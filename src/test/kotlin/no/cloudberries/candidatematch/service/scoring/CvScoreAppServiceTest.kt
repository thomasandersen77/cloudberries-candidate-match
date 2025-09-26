package no.cloudberries.candidatematch.service.scoring

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.entities.scoring.CvScoreEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.scoring.CvScoreRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CvScoreAppServiceTest {

    private val consultantRepository: ConsultantRepository = mockk()
    private val cvScoreRepository: CvScoreRepository = mockk(relaxed = true)
    private val scoreCandidateService: ScoreCandidateService = mockk()

    private val service = CvScoreAppService(consultantRepository, cvScoreRepository, scoreCandidateService)

    @Test
    fun `score single candidate stores and returns dto`() {
        val resume = JsonNodeFactory.instance.objectNode().put("email", "a@b.com").put("bornYear", 1990)
        val consultant = ConsultantEntity(
            id = 1L,
            userId = "u1",
            name = "Alice",
            cvId = "cv1",
            resumeData = resume,
            skills = emptySet()
        )
        every { consultantRepository.findByUserId("u1") } returns consultant
        every { cvScoreRepository.findByCandidateUserId("u1") } returns null
every { scoreCandidateService.performCvScoring(any(), any(), any()) } returns no.cloudberries.candidatematch.domain.scoring.CVEvaluation(
            name = "Alice",
            summary = "Good CV",
            strengths = listOf("Clean code"),
            improvements = listOf("More tests"),
            scoreBreakdown = null,
            scorePercentage = 85
        )
        every { cvScoreRepository.save(any()) } answers { firstArg<CvScoreEntity>().copy(id = 10L) }

        val dto = service.scoreCandidate("u1")

        assertEquals("u1", dto.candidateId)
        assertEquals(85, dto.scorePercent)
        assertEquals("Good CV", dto.summary)
        verify { cvScoreRepository.save(any()) }
    }

    @Test
    fun `score all returns processed count`() {
        val resume = JsonNodeFactory.instance.objectNode().put("email", "a@b.com").put("bornYear", 1990)
        val consultant = ConsultantEntity(
            id = 1L,
            userId = "u1",
            name = "Alice",
            cvId = "cv1",
            resumeData = resume,
            skills = emptySet()
        )
        every { consultantRepository.findAll() } returns listOf(consultant)
        every { consultantRepository.findByUserId("u1") } returns consultant
        every { cvScoreRepository.findByCandidateUserId("u1") } returns null
every { scoreCandidateService.performCvScoring(any(), any(), any()) } returns no.cloudberries.candidatematch.domain.scoring.CVEvaluation(
            name = "Alice",
            summary = "Great",
            strengths = emptyList(),
            improvements = emptyList(),
            scoreBreakdown = null,
            scorePercentage = 90
        )
        every { cvScoreRepository.save(any()) } answers { firstArg<CvScoreEntity>().copy(id = 11L) }

        val res = service.scoreAll()
        assertEquals(1, res.processedCount)
    }
}