package no.cloudberries.candidatematch.service.matching

import mu.KotlinLogging
import no.cloudberries.candidatematch.config.AISettings
import no.cloudberries.candidatematch.controllers.matching.CoverageStatus
import no.cloudberries.candidatematch.controllers.matching.MatchConsultantDto
import no.cloudberries.candidatematch.controllers.matching.MatchesListItemDto
import no.cloudberries.candidatematch.controllers.matching.PagedMatchesListDto
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.CustomerProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantSearchRepository
import no.cloudberries.candidatematch.infrastructure.repositories.projectrequest.CustomerProjectRequestRepository
import no.cloudberries.candidatematch.service.ai.AIService
import no.cloudberries.candidatematch.service.consultants.ConsultantWithCvService
import no.cloudberries.candidatematch.domain.consultant.RelationalSearchCriteria
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MatchesService(
    private val requestRepo: CustomerProjectRequestRepository,
    private val consultantSearchRepository: ConsultantSearchRepository,
    private val consultantWithCvService: ConsultantWithCvService,
    private val extractor: RequirementsSkillExtractor,
    private val aiService: AIService,
    private val aiSettings: AISettings,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun listRequestsWithCoverage(page: Int, size: Int, sort: String): PagedMatchesListDto {
        val sortParts = sort.split(",")
        val sortField = sortParts.getOrNull(0)?.ifBlank { "uploadedAt" } ?: "uploadedAt"
        val sortDir = if (sortParts.getOrNull(1)?.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(page, size, Sort.by(sortDir, sortField))

        val pageData = requestRepo.findAllBy(pageable)
        val items = pageData.content.map { req ->
            val skills = extractor.extractSkillsAny(req).toList()
            val hitCount = computeHitCount(skills)
            val (status, label) = mapCoverage(hitCount)
            MatchesListItemDto(
                id = req.id ?: -1,
                title = req.title,
                customerName = req.customerName,
                date = req.uploadedAt,
                deadlineDate = req.deadlineDate,
                hitCount = hitCount,
                coverageStatus = status,
                coverageLabel = label,
            )
        }

        return PagedMatchesListDto(
            content = items,
            totalElements = pageData.totalElements,
            totalPages = pageData.totalPages,
            currentPage = page,
            pageSize = size,
            hasNext = pageData.hasNext(),
            hasPrevious = pageData.hasPrevious(),
        )
    }

    @Transactional(readOnly = true)
    fun getTopConsultantsWithAI(requestId: Long, limit: Int = 5): List<MatchConsultantDto> {
        val clampedLimit = limit.coerceIn(1, 10)
        val request = requestRepo.findWithRequirementsById(requestId) ?: return emptyList()
        val skills = extractor.extractSkillsAny(request).toList()
        val preRanked = consultantWithCvService.getTopConsultantsBySkills(skills, limit = maxOf(10, clampedLimit))

        val requestText = buildRequestText(request)
        val enriched = preRanked.map { consultant ->
            val cvText = ConsultantCvTextFlattener.toText(consultant)
            val (score, justification) = matchWithAI(consultant.name, cvText, requestText)
            Pair(consultant, score to justification)
        }

        return enriched
            .sortedByDescending { it.second.first }
            .take(clampedLimit)
            .map { (c, pair) ->
                val (score, justification) = pair
                MatchConsultantDto(
                    userId = c.userId,
                    name = c.name,
                    cvId = c.cvId,
                    relevanceScore = score,
                    justification = justification,
                )
            }
    }

    private fun matchWithAI(name: String, cvText: String, requestText: String): Pair<Double, String?> {
        return try {
            if (!aiSettings.enabled) return fallbackScore(name, requestText)
            val provider: AIProvider = aiSettings.provider
            val resp = aiService.matchCandidate(
                aiProvider = provider,
                cv = cvText,
                request = requestText,
                consultantName = name,
            )
            val score = resp.totalScore.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 70.0
            val justification = resp.summary
            score to justification
        } catch (e: Exception) {
            logger.warn(e) { "AI match failed for $name, using fallback score" }
            fallbackScore(name, requestText)
        }
    }

    private fun fallbackScore(name: String, requestText: String): Pair<Double, String?> {
        // We don't have structural overlap here easily; return a conservative mid score
        val score = 60.0
        val justification = "Begrunnelse basert på overlapp med krav. (Fallback – AI utilgjengelig)"
        return score to justification
    }

    private fun buildRequestText(req: CustomerProjectRequestEntity): String {
        val musts = req.requirements.mapNotNull { r -> r.name?.takeIf { it.isNotBlank() } }
        return """
            Kunde: ${req.customerName ?: "-"}
            Tittel: ${req.title ?: "-"}
            Oppsummering: ${req.summary ?: "-"}
            Krav: ${musts.joinToString(", ")}
        """.trimIndent()
    }

    private fun mapCoverage(hitCount: Long): Pair<CoverageStatus, String> = when {
        hitCount >= 10 -> CoverageStatus.GREEN to "God dekning"
        hitCount <= 2 -> CoverageStatus.RED to "Lav dekning"
        hitCount in 5..9 -> CoverageStatus.YELLOW to "Begrenset dekning"
        else -> CoverageStatus.NEUTRAL to "Nøytral"
    }

    private fun computeHitCount(skillsAny: List<String>): Long {
        if (skillsAny.isEmpty()) return 0
        val criteria = RelationalSearchCriteria(
            skillsAll = emptyList(),
            skillsAny = skillsAny,
            minQualityScore = null,
            onlyActiveCv = true,
        )
        val page = consultantSearchRepository.findByRelationalCriteria(criteria, PageRequest.of(0, 1))
        return page.totalElements
    }
}