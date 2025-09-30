package no.cloudberries.candidatematch.service.projectrequest

import mu.KotlinLogging
import no.cloudberries.candidatematch.config.ProjectRequestAnalysisConfig
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.CustomerProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.ProjectRequestRequirementEntity
import no.cloudberries.candidatematch.infrastructure.repositories.projectrequest.CustomerProjectRequestRepository
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import no.cloudberries.candidatematch.service.projectrequest.parser.RequirementParser
import no.cloudberries.candidatematch.templates.AnalyzeCustomerRequestPromptTemplate
import no.cloudberries.candidatematch.templates.ProjectRequestParams
import no.cloudberries.candidatematch.templates.renderProjectRequestTemplate
import no.cloudberries.candidatematch.utils.PdfUtils
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream

@Service
class ProjectRequestAnalysisService(
    private val customerProjectRequestRepository: CustomerProjectRequestRepository,
    private val requirementRepository: no.cloudberries.candidatematch.infrastructure.repositories.projectrequest.ProjectRequestRequirementRepository,
    private val requirementParser: RequirementParser,
    private val aiAnalysisService: AIAnalysisService,
    private val analysisConfig: ProjectRequestAnalysisConfig,
) {
    private val logger = KotlinLogging.logger {}

    data class Aggregate(
        val request: CustomerProjectRequestEntity,
        val requirements: List<ProjectRequestRequirementEntity>
    )

    @Timed
    @Transactional
    fun analyzeAndStore(
        pdfStream: InputStream,
        originalFilename: String? = null,
    ): Aggregate {
        val text = PdfUtils.extractText(pdfStream).trim()
        val (title, fallbackSummary) = deriveTitleAndSummary(text)

        // Optional AI analysis for better summary (and possibly structured extraction in the future)
        val summary = if (analysisConfig.aiEnabled) {
            try {
                val prompt = renderProjectRequestTemplate(
                    AnalyzeCustomerRequestPromptTemplate.template,
                    ProjectRequestParams(requestText = text)
                )
                val aiResp = aiAnalysisService.analyzeContent(prompt, analysisConfig.provider)
                aiResp.content.trim().ifBlank { fallbackSummary }
            } catch (e: Exception) {
                logger.warn(e) { "AI analysis failed; falling back to derived summary" }
                fallbackSummary
            }
        } else fallbackSummary

        val parsed = requirementParser.parse(text)
        val request = CustomerProjectRequestEntity(
            customerName = "Imported",
            title = title,
            summary = summary,
            originalFilename = originalFilename,
            originalText = text,
            requirements = emptyList()
        )
        val saved = customerProjectRequestRepository.save(request)
        // Attach requirements to saved parent and persist
        val toPersist = parsed.map { r ->
            ProjectRequestRequirementEntity(
                projectRequest = saved,
                name = r.name,
                details = r.details,
                priority = r.priority,
            )
        }
        val savedReqs = requirementRepository.saveAll(toPersist)
        logger.info { "Stored customer project request id=${saved.id}, reqCount=${savedReqs.size}" }
        return Aggregate(saved.copy(requirements = savedReqs), savedReqs)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): Aggregate? =
        customerProjectRequestRepository.findWithRequirementsById(id)?.let { Aggregate(it, it.requirements) }

    @Transactional(readOnly = true)
    fun listAll(): List<Aggregate> =
        customerProjectRequestRepository.findAllBy().map { Aggregate(it, it.requirements) }

    private fun deriveTitleAndSummary(text: String): Pair<String?, String?> {
        val trimmed = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val title = trimmed.firstOrNull()?.take(200)
        val summary = text.take(1000)
        return title to summary
    }
}
