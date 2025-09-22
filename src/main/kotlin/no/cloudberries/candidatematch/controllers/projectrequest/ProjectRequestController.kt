package no.cloudberries.candidatematch.controllers.projectrequest

import no.cloudberries.candidatematch.domain.projectrequest.CustomerProjectRequest
import no.cloudberries.candidatematch.infrastructure.entities.RequirementPriority
import no.cloudberries.candidatematch.service.projectrequest.ProjectRequestAnalysisService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/project-requests")
class ProjectRequestController(
    private val analysisService: ProjectRequestAnalysisService
) {

    @PostMapping(
        path = ["/upload"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun uploadAndAnalyze(
        @RequestPart("file") file: MultipartFile
    ): ProjectRequestResponseDto {
        val projectRequest = analysisService.analyzeAndStore(
            pdfStream = file.inputStream,
            originalFilename = file.originalFilename
        )
        return projectRequest.toDto()
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ProjectRequestResponseDto? =
        analysisService.getById(id)?.toDto()
}

// DTOs and mappers

data class ProjectRequestResponseDto(
    val id: Long,
    val customerName: String?,
    val title: String?,
    val summary: String?,
    val mustRequirements: List<ProjectRequirementDto>,
    val shouldRequirements: List<ProjectRequirementDto>,
    val originalFilename: String?,
)

data class ProjectRequirementDto(
    val name: String,
    val details: String?
)

fun CustomerProjectRequest.toDto(): ProjectRequestResponseDto {
    val must = this.requirements.filter { it.priority == RequirementPriority.MUST }
        .map {
            ProjectRequirementDto(
                it.name,
                it.details
            )
        }
    val should = this.requirements.filter { it.priority == RequirementPriority.SHOULD }
        .map {
            ProjectRequirementDto(
                it.name,
                it.details
            )
        }
    return ProjectRequestResponseDto(
        id = this.id?.value ?: -1,
        customerName = this.customerName,
        title = this.title,
        summary = this.summary,
        mustRequirements = must,
        shouldRequirements = should,
        originalFilename = this.originalFilename
    )
}