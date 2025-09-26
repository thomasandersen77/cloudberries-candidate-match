package no.cloudberries.candidatematch.service.projectrequest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.cloudberries.candidatematch.domain.ai.AIProvider
import no.cloudberries.candidatematch.domain.projectrequest.CustomerProjectRequest
import no.cloudberries.candidatematch.domain.projectrequest.CustomerProjectRequirement
import no.cloudberries.candidatematch.domain.projectrequest.ProjectRequestAIResponse
import no.cloudberries.candidatematch.infrastructure.entities.RequirementPriority
import no.cloudberries.candidatematch.infrastructure.entities.fromDomain
import no.cloudberries.candidatematch.infrastructure.entities.toDomain
import no.cloudberries.candidatematch.infrastructure.repositories.CustomerProjectRequestRepository
import no.cloudberries.candidatematch.service.ai.AIAnalysisService
import no.cloudberries.candidatematch.templates.ProjectRequestParams
import no.cloudberries.candidatematch.templates.ProjectRequestPromptTemplate
import no.cloudberries.candidatematch.templates.renderProjectRequestTemplate
import no.cloudberries.candidatematch.utils.PdfUtils
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream

@Service
class ProjectRequestAnalysisService(
    private val aiAnalysisService: AIAnalysisService,
    private val customerProjectRequestRepository: CustomerProjectRequestRepository,
) {
    private val logger = KotlinLogging.logger {}
    private val mapper = jacksonObjectMapper()

    @Timed
    @Transactional
    fun analyzeAndStore(
        pdfStream: InputStream,
        originalFilename: String? = null,
        aiProvider: AIProvider = AIProvider.GEMINI,
    ): CustomerProjectRequest {
        val text = PdfUtils.extractText(pdfStream)
        val prompt = renderProjectRequestTemplate(
            ProjectRequestPromptTemplate.template,
            ProjectRequestParams(requestText = text)
        )

        val response = aiAnalysisService.analyzeContent(
            prompt,
            aiProvider
        )

        val ai = mapper.readValue<ProjectRequestAIResponse>(response.content).projectRequest

        val must = ai.mustRequirements.map {
            CustomerProjectRequirement(
                name = it.name,
                details = it.details,
                priority = RequirementPriority.MUST
            )
        }
        val should = ai.shouldRequirements.map {
            CustomerProjectRequirement(
                name = it.name,
                details = it.details,
                priority = RequirementPriority.SHOULD
            )
        }

        val entity = customerProjectRequestRepository.save(
            CustomerProjectRequest(
                id = null,
                customerName = ai.customerName,
                title = ai.title,
                summary = ai.summary,
                originalFilename = originalFilename,
                originalText = text,
                requirements = must + should
            ).fromDomain()
        )
        logger.info { "Stored customer project request id=${entity.id}" }
        return entity.toDomain()
    }

    fun getById(id: Long): CustomerProjectRequest? =
        customerProjectRequestRepository.findById(id).orElse(null)?.toDomain()
}