package no.cloudberries.candidatematch.controllers.projectrequest

import jakarta.validation.Valid
import mu.KotlinLogging
import no.cloudberries.candidatematch.service.ProjectRequestService
import no.cloudberries.candidatematch.service.projectrequest.ProjectRequestAnalysisService
import no.cloudberries.candidatematch.utils.Timed
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/project-requests")
class ProjectRequestController(
    private val analysisService: ProjectRequestAnalysisService,
    private val projectRequestService: ProjectRequestService
) {

    private val logger = KotlinLogging.logger { }

    @Timed
    @PostMapping(
        path = ["/upload"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun uploadAndAnalyze(
        @RequestPart("file") file: MultipartFile
    ): ProjectRequestResponseDto {
        logger.info { "Uploaded filename='${file.originalFilename}' size=${file.size.fromBytesToMB()} kB" }
        val agg = analysisService.analyzeAndStore(
            pdfStream = file.inputStream,
            originalFilename = file.originalFilename
        )
        return agg.toDto()
    }

    @Timed
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ProjectRequestResponseDto? {
        logger.info { "GET /api/project-requests/$id" }
        return analysisService.getById(id)?.toDto()
    }

    @Timed
    @GetMapping
    fun listAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "id,desc") sort: String
    ): ResponseEntity<PagedProjectRequestResponseDto> {
        logger.info { "GET /api/project-requests (page=$page, size=$size, sort=$sort)" }

        val sortParams = sort.split(",")
        val sortField = sortParams.getOrNull(0) ?: "id"
        val sortDirection =
            if (sortParams.getOrNull(1)?.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC

        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                sortDirection,
                sortField
            )
        )
        val projectRequestPage = projectRequestService.findAll(pageable)

        // Convert to legacy format for compatibility
        val analysisResults = analysisService.listAll().map { it.toDto() }

        val response = PagedProjectRequestResponseDto(
            content = analysisResults,
            totalElements = projectRequestPage.totalElements,
            totalPages = projectRequestPage.totalPages,
            currentPage = page,
            pageSize = size,
            hasNext = projectRequestPage.hasNext(),
            hasPrevious = projectRequestPage.hasPrevious()
        )

        return ResponseEntity.ok(response)
    }

    @Timed
    @PostMapping
    fun createProjectRequest(
        @Valid @RequestBody request: CreateProjectRequestDto
    ): ResponseEntity<ProjectRequestDto> {
        logger.info { "POST /api/project-requests for customer ${request.customerName}" }

        val projectRequest = projectRequestService.createProjectRequest(
            customerName = request.customerName,
            requiredSkills = request.requiredSkills,
            startDate = request.startDate,
            endDate = request.endDate,
            responseDeadline = request.responseDeadline,
            status = request.status ?: no.cloudberries.candidatematch.infrastructure.entities.RequestStatus.OPEN,
            requestDescription = request.requestDescription,
            responsibleSalespersonEmail = request.responsibleSalespersonEmail
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(projectRequest.toDto())
    }

    @Timed
    @PutMapping("/{id}/close")
    fun closeProjectRequest(@PathVariable id: Long): ResponseEntity<ProjectRequestDto> {
        logger.info { "PUT /api/project-requests/$id/close" }

        val closedRequest = projectRequestService.closeProjectRequest(id)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(closedRequest.toDto())
    }

    @Timed
    @PostMapping("/{id}/analyze")
    fun analyzeProjectRequest(@PathVariable id: Long): ResponseEntity<ProjectRequestDto> {
        logger.info { "POST /api/project-requests/$id/analyze" }

        val analyzedRequest = projectRequestService.analyzeProjectRequest(id)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(analyzedRequest.toDto())
    }

    @Timed
    @GetMapping("/{id}/suggestions")
    fun getProjectRequestSuggestions(@PathVariable id: Long): ResponseEntity<List<AISuggestionDto>> {
        logger.info { "GET /api/project-requests/$id/suggestions" }

        val projectRequest = projectRequestService.findById(id)
            ?: return ResponseEntity.notFound().build()

        val suggestions = projectRequest.aISuggestions.map { it.toDto() }
        return ResponseEntity.ok(suggestions)
    }
}

private fun Long.fromBytesToMB(): Int {
    return (this / 1024).toInt()
}
