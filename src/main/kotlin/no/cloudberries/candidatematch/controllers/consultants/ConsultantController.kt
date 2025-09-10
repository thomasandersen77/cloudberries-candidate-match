package no.cloudberries.candidatematch.controllers.consultants

import no.cloudberries.candidatematch.controllers.consultants.dto.AssignmentDto
import no.cloudberries.candidatematch.controllers.consultants.dto.ConsultantSummaryDto
import no.cloudberries.candidatematch.controllers.consultants.dto.CvDto
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.entities.consultant.ConsultantCvEntity
import no.cloudberries.candidatematch.infrastructure.entities.consultant.ProjectAssignmentEntity
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ConsultantCvRepository
import no.cloudberries.candidatematch.service.consultants.ConsultantAssignmentService
import no.cloudberries.candidatematch.service.consultants.ConsultantCvService
import no.cloudberries.candidatematch.service.consultants.LiquidityReductionService
import no.cloudberries.candidatematch.infrastructure.repositories.consultant.ProjectAssignmentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.YearMonth

@RestController
@RequestMapping("/api/consultants")
class ConsultantController(
    private val consultantRepository: ConsultantRepository,
    private val consultantCvRepository: ConsultantCvRepository,
    private val projectAssignmentRepository: ProjectAssignmentRepository,
    private val cvService: ConsultantCvService,
    private val assignmentService: ConsultantAssignmentService,
    private val liquidityReductionService: LiquidityReductionService
) {
    // Consultants with pagination & filtering
    @GetMapping
    fun listConsultants(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) userId: String?,
        pageable: Pageable
    ): Page<ConsultantSummaryDto> {
        val spec: Specification<ConsultantEntity> = Specification.where<ConsultantEntity>(null)
            .and(name?.takeIf { it.isNotBlank() }?.let { n -> Specification { root, _, cb ->
                cb.like(cb.lower(root.get("name")), "%" + n.lowercase() + "%")
            } })
            .and(userId?.takeIf { it.isNotBlank() }?.let { uid -> Specification { root, _, cb ->
                cb.equal(root.get<String>("userId"), uid)
            } })
            ?: Specification.where(null)

        return consultantRepository.findAll(spec, pageable).map { c ->
            val activeCv = consultantCvRepository.findByConsultantIdAndActiveTrue(c.id!!)
            ConsultantDtoMapper.toSummaryDto(c, activeCv)
        }
    }

    @GetMapping("/{consultantId}")
    fun getConsultant(@PathVariable consultantId: Long): ConsultantSummaryDto {
        val c = consultantRepository.findById(consultantId).orElseThrow()
        val activeCv = consultantCvRepository.findByConsultantIdAndActiveTrue(consultantId)
        return ConsultantDtoMapper.toSummaryDto(c, activeCv)
    }

    // CVs with pagination & filtering
    @GetMapping("/{consultantId}/cvs")
    fun listCvs(
        @PathVariable consultantId: Long,
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) versionTag: String?,
        pageable: Pageable
    ): Page<CvDto> {
        val specBase: Specification<ConsultantCvEntity> = Specification { root, _, cb ->
            cb.equal(root.get<Long>("consultant").get<Long>("id"), consultantId)
        }
        val spec = specBase
            .and(active?.let { a -> Specification<ConsultantCvEntity> { root, _, cb -> cb.equal(root.get<Boolean>("active"), a) } })
            .and(versionTag?.takeIf { it.isNotBlank() }?.let { vt -> Specification<ConsultantCvEntity> { root, _, cb ->
                cb.like(cb.lower(root.get("versionTag")), "%" + vt.lowercase() + "%")
            } })

        return consultantCvRepository.findAll(spec, pageable).map { ConsultantDtoMapper.toCvDto(it) }
    }

    @PostMapping("/{consultantId}/cvs")
    fun createCv(@PathVariable consultantId: Long, @RequestBody dto: CvDto): CvDto =
        ConsultantDtoMapper.toCvDto(cvService.createCv(consultantId, dto))

    @PatchMapping("/{consultantId}/cvs/{cvId}/activate")
    fun activateCv(@PathVariable consultantId: Long, @PathVariable cvId: Long) {
        cvService.activate(consultantId, cvId)
    }

    // Assignments with pagination & filtering
    @GetMapping("/{consultantId}/assignments")
    fun listAssignments(
        @PathVariable consultantId: Long,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) billable: Boolean?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        pageable: Pageable
    ): Page<AssignmentDto> {
        val specBase: Specification<ProjectAssignmentEntity> = Specification { root, _, cb ->
            cb.equal(root.get<Long>("consultant").get<Long>("id"), consultantId)
        }
        val spec = specBase
            .and(title?.takeIf { it.isNotBlank() }?.let { t -> Specification<ProjectAssignmentEntity> { root, _, cb ->
                cb.like(cb.lower(root.get("title")), "%" + t.lowercase() + "%")
            } })
            .and(billable?.let { b -> Specification<ProjectAssignmentEntity> { root, _, cb -> cb.equal(root.get<Boolean>("billable"), b) } })
            .and(
                if (from != null || to != null) Specification<ProjectAssignmentEntity> { root, _, cb ->
                    val start = root.get<LocalDate>("startDate")
                    val end = root.get<LocalDate>("endDate")
                    val upper = to ?: LocalDate.MAX
                    val lower = from ?: LocalDate.MIN
                    cb.and(
                        cb.lessThanOrEqualTo(start, upper),
                        cb.or(cb.isNull(end), cb.greaterThanOrEqualTo(end, lower))
                    )
                } else null
            )

        val page = projectAssignmentRepository.findAll(spec, pageable)
        return page.map {
            AssignmentDto(
                id = it.id,
                title = it.title,
                startDate = it.startDate,
                endDate = it.endDate,
                allocationPercent = it.allocationPercent,
                hourlyRate = it.hourlyRate,
                costRate = it.costRate,
                clientProjectRef = it.clientProjectRef,
                billable = it.billable,
            )
        }
    }

    @PostMapping("/{consultantId}/assignments")
    fun createAssignment(@PathVariable consultantId: Long, @RequestBody dto: AssignmentDto): AssignmentDto {
        val created = assignmentService.create(consultantId, dto)
        return AssignmentDto(
            id = created.id,
            title = created.title,
            startDate = created.startDate,
            endDate = created.endDate,
            allocationPercent = created.allocationPercent,
            hourlyRate = created.hourlyRate,
            costRate = created.costRate,
            clientProjectRef = created.clientProjectRef,
            billable = created.billable,
        )
    }

    @PutMapping("/{consultantId}/assignments/{assignmentId}")
    fun updateAssignment(
        @PathVariable consultantId: Long,
        @PathVariable assignmentId: Long,
        @RequestBody dto: AssignmentDto
    ): AssignmentDto {
        val updated = assignmentService.update(consultantId, assignmentId, dto)
        return AssignmentDto(
            id = updated.id,
            title = updated.title,
            startDate = updated.startDate,
            endDate = updated.endDate,
            allocationPercent = updated.allocationPercent,
            hourlyRate = updated.hourlyRate,
            costRate = updated.costRate,
            clientProjectRef = updated.clientProjectRef,
            billable = updated.billable,
        )
    }

    @DeleteMapping("/{consultantId}/assignments/{assignmentId}")
    fun deleteAssignment(@PathVariable consultantId: Long, @PathVariable assignmentId: Long) {
        assignmentService.delete(consultantId, assignmentId)
    }

    @GetMapping("/{consultantId}/liquidity-reduction")
    fun liquidityReduction(
        @PathVariable consultantId: Long,
        @RequestParam("yearMonth") @DateTimeFormat(pattern = "yyyy-MM") yearMonth: YearMonth
    ): Map<String, Any> {
        val amount = liquidityReductionService.calculateLiquidityReductionForMonth(consultantId, yearMonth)
        return mapOf("consultantId" to consultantId, "yearMonth" to yearMonth.toString(), "amount" to amount)
    }
}

