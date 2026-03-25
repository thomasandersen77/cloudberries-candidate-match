package no.cloudberries.candidatematch.service.projectrequest

import mu.KotlinLogging
import no.cloudberries.ai.port.AnalyzedProjectRequest
import no.cloudberries.ai.port.ProjectRequestAnalysisPort
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.CustomerProjectRequestEntity
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.ProjectRequestRequirementEntity
import no.cloudberries.candidatematch.infrastructure.entities.projectrequest.RequirementPriority
import no.cloudberries.candidatematch.infrastructure.repositories.projectrequest.CustomerProjectRequestRepository
import no.cloudberries.candidatematch.utils.PdfUtils
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.LocalDateTime

@Service
class ProjectRequestAnalysisService(
    private val customerProjectRequestRepository: CustomerProjectRequestRepository,
    private val requirementRepository: no.cloudberries.candidatematch.infrastructure.repositories.projectrequest.ProjectRequestRequirementRepository,
    private val projectRequestAnalysisPort: ProjectRequestAnalysisPort
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

        val processedAI = try {
            projectRequestAnalysisPort.analyzeProjectRequest(text, originalFilename)
        } catch (e: Exception) {
            logger.warn(e) { "AI analysis failed; returning empty analysis" }
            AnalyzedProjectRequest(
                customerName = "Unknown Customer",
                summary = text.take(1000),
                mustRequirements = emptyList(),
                shouldRequirements = emptyList(),
                uploadedAt = LocalDateTime.now(),
                deadlineDate = null
            )
        }

        val request = CustomerProjectRequestEntity(
            customerName = processedAI.customerName ?: "Unknown Customer",
            title = text.lines().firstOrNull { it.isNotBlank() }?.take(200),
            summary = processedAI.summary ?: text.take(1000),
            originalFilename = originalFilename,
            originalText = text,
            deadlineDate = processedAI.deadlineDate,
            requirements = emptyList()
        )
        val saved = customerProjectRequestRepository.save(request)

        val mustReqs = processedAI.mustRequirements.map {
            ProjectRequestRequirementEntity(
                projectRequest = saved,
                name = it.trim(),
                details = null,
                priority = RequirementPriority.MUST
            )
        }
        val shouldReqs = processedAI.shouldRequirements.map {
            ProjectRequestRequirementEntity(
                projectRequest = saved,
                name = it.trim(),
                details = null,
                priority = RequirementPriority.SHOULD
            )
        }

        val savedReqs = requirementRepository.saveAll(mustReqs + shouldReqs)
        logger.info { "Stored customer project request id=${saved.id}, customer='${saved.customerName}', reqCount=${savedReqs.size}" }
        return Aggregate(saved.copy(requirements = savedReqs), savedReqs)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): Aggregate? =
        customerProjectRequestRepository.findWithRequirementsById(id)?.let { Aggregate(it, it.requirements) }

    @Transactional(readOnly = true)
    fun listAll(): List<Aggregate> =
        customerProjectRequestRepository.findAllBy().map { Aggregate(it, it.requirements) }
}
