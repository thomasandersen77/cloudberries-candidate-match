package no.cloudberries.candidatematch.service.matching

import mu.KotlinLogging
import no.cloudberries.ai.config.AISettings
import no.cloudberries.candidatematch.controllers.matching.CoverageStatus
import no.cloudberries.candidatematch.controllers.matching.MatchConsultantDto
import no.cloudberries.candidatematch.controllers.matching.MatchesListItemDto
import no.cloudberries.candidatematch.controllers.matching.PagedMatchesListDto
import no.cloudberries.ai.domain.AIProvider
import no.cloudberries.ai.port.CandidateMatchingPort
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
    private val consultantScoringService: ConsultantScoringService,
    private val candidateMatchingPort: CandidateMatchingPort
) {
    private val logger = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun listRequestsWithCoverage(page: Int, size: Int, sort: String): PagedMatchesListDto {
        val sortParts = sort.split(",")
        val rawField = sortParts.getOrNull(0)?.ifBlank { "uploadedAt" } ?: "uploadedAt"
        val sortField = when (rawField) {
            "date" -> "uploadedAt"
            "deadline" -> "deadlineDate"
            else -> rawField
        }
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
        
        logger.info { "[MATCHING START] Request $requestId requires consultants with skills: ${skills.joinToString(", ")}" }
        
        val preRanked = consultantWithCvService.getTopConsultantsBySkills(skills, limit = maxOf(10, clampedLimit))

        val requestText = buildRequestText(request)
        val enriched = preRanked.map { consultant ->
            val cvText = ConsultantCvTextFlattener.toText(consultant)
            val matchResponse = candidateMatchingPort.matchCandidate(
                cv = cvText,
                request = requestText,
                consultantName = consultant.name,
                provider = aiSettings.provider
            )
            val score = matchResponse.totalScore
            val justification = matchResponse.summary
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
                    relevanceScore = score.toDouble(),
                    justification = justification,
                )
            }
    }

    fun matchWithAI(name: String, cvText: String, requestText: String): Pair<Double, String?> {
        val response = aiService.matchCandidate(
            aiProvider = aiSettings.provider,
            cv = cvText,
            request = requestText,
            consultantName = name
        )
        return response.totalScore.toDouble() to response.summary
    }

    fun fallbackScore(name: String, requestText: String): Pair<Double, String?> {
        return 0.0 to "Fallback score used (no AI match performed)"
    }

    fun buildRequestText(req: CustomerProjectRequestEntity): String {
        val sb = StringBuilder()
        sb.append("Customer: ${req.customerName}\n")
        sb.append("Title: ${req.title}\n")
        sb.append("Summary: ${req.summary}\n")
        sb.append("Requirements:\n")
        req.requirements.forEach { r -> sb.append("- ${r.name} (${r.priority})\n") }
        return sb.toString()
    }

    private fun mapCoverage(hitCount: Long): Pair<CoverageStatus, String> {
        return when {
            hitCount >= 5 -> CoverageStatus.GREEN to "High Coverage"
            hitCount >= 2 -> CoverageStatus.YELLOW to "Medium Coverage"
            hitCount > 0 -> CoverageStatus.RED to "Low Coverage"
            else -> CoverageStatus.NEUTRAL to "No matches"
        }
    }

    private fun computeHitCount(skillsAny: List<String>): Long {
        if (skillsAny.isEmpty()) return 0
        val criteria = RelationalSearchCriteria(
            skillsAny = skillsAny
        )
        return consultantSearchRepository.findByRelationalCriteria(criteria, PageRequest.of(0, 50)).totalElements
    }
    @Transactional(readOnly = true)
    fun reAnalyzeWithGemini(requestId: Long): List<MatchConsultantDto> {
        return getTopConsultantsWithAI(requestId)
    }

    fun extractCvQuality(consultant: no.cloudberries.candidatematch.controllers.consultants.ConsultantWithCvDto): Int {
        return 75 // Default fallback
    }
}
