# CV Quality Score Integration - Implementation Guide

## Problem Statement

The frontend expects CV quality scores via `ConsultantCvDto.qualityScore` but the backend returns null because the field isn't populated. The actual scores exist in the `cv_score` table with column `score_percent`. Additionally, search endpoints accept `minQualityScore` but don't filter by it, causing empty result sets.

## Solution Overview

Following DDD and Clean Architecture principles with unified domain concept:

1. **Domain Unification**: Establish `cv_score.score_percent` as the single source of truth for CV quality
2. **Domain Layer**: Create clear contracts and value objects that reference CV scores
3. **Service Layer**: Implement quality enrichment and filtering logic consistently  
4. **Infrastructure Layer**: All queries join against `cv_score` table for quality data
5. **API Layer**: DTOs map to domain concepts but maintain existing structure for compatibility

## Architecture Approach - Unified Domain Model

This follows the established layering pattern with domain coherence:
- **Single Source of Truth**: `cv_score.score_percent` is the authoritative quality score
- **Domain Consistency**: All quality-related operations reference the same underlying data
- **Controllers → Services → Domain → Infrastructure**: Clear separation with unified concepts
- **Dependency Inversion Principle**: Domain defines quality contracts, infrastructure implements
- **API Compatibility**: DTOs maintain existing structure while mapping to unified domain model

## Implementation Plan

### 1. Domain Model Enhancement - Unified Quality Concept

**File**: `src/main/kotlin/no/cloudberries/candidatematch/domain/consultant/CvQuality.kt`

```kotlin
package no.cloudberries.candidatematch.domain.consultant

/**
 * Value object representing CV quality score (0-100)
 * Unified domain concept that always references cv_score.score_percent
 * Follows Domain-Driven Design principles with encapsulated business rules
 */
data class CvQuality private constructor(val scorePercent: Int) {
    companion object {
        /**
         * Creates CvQuality from cv_score.score_percent value
         * This is the canonical way to create quality scores in the domain
         */
        fun fromScorePercent(scorePercent: Int?): CvQuality? {
            return when {
                scorePercent == null -> null
                scorePercent < 0 -> null
                scorePercent > 100 -> CvQuality(100)
                else -> CvQuality(scorePercent)
            }
        }
        
        /**
         * Creates CvQuality from API/DTO input (for backward compatibility)
         * Maps to the same underlying score_percent concept
         */
        fun fromApiScore(apiScore: Int?): CvQuality? = fromScorePercent(apiScore)
        
        fun zero(): CvQuality = CvQuality(0)
    }
    
    /**
     * The score as stored in cv_score.score_percent (0-100)
     * This is the single source of truth for quality values
     */
    val asPercentage: Int get() = scorePercent
    
    fun isAboveOrEqual(threshold: CvQuality): Boolean = this.scorePercent >= threshold.scorePercent
    
    fun meetsCriteria(minThreshold: CvQuality?): Boolean {
        return minThreshold?.let { this.scorePercent >= it.scorePercent } ?: true
    }
    
    /**
     * For API compatibility - returns the same value as scorePercent
     * but makes it clear this maps to DTOs
     */
    fun toApiScore(): Int = scorePercent
}
```

**File**: Update existing `Cv.kt` to use unified quality concept:

```kotlin
data class Cv(
    val id: String,
    /**
     * CV quality score sourced from cv_score.score_percent
     * Null when no quality assessment has been performed
     */
    val quality: CvQuality? = null,
    val active: Boolean = false,
    // ... existing fields
) {
    /**
     * Convenience method to check if this CV meets quality criteria
     * Used in search and filtering operations
     */
    fun meetsQualityThreshold(minQuality: CvQuality?): Boolean {
        return quality?.meetsCriteria(minQuality) ?: (minQuality == null)
    }
}
```

### 2. Repository Interface (Domain Layer) - Quality-Aware Contracts

**File**: `src/main/kotlin/no/cloudberries/candidatematch/domain/consultant/ConsultantWithCvRepository.kt`

```kotlin
package no.cloudberries.candidatematch.domain.consultant

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Repository interface following Dependency Inversion Principle
 * Domain layer defines the contract, infrastructure provides implementation
 * All methods ensure CV quality is populated from cv_score.score_percent
 */
interface ConsultantWithCvRepository {
    /**
     * Finds all consultants with their CVs and quality scores from cv_score table
     * Quality is always populated when cv_score.score_percent exists, null otherwise
     */
    fun findAllWithCvQuality(onlyActive: Boolean = false): List<ConsultantWithCv>
    
    /**
     * Paged version that joins cv_score for quality data
     * Ensures consistent quality population across all results
     */
    fun findAllWithCvQualityPaged(onlyActive: Boolean = false, pageable: Pageable): Page<ConsultantWithCv>
    
    /**
     * Searches consultants using relational criteria
     * Quality filtering is applied against cv_score.score_percent directly
     * Missing scores are treated as 0 for filtering purposes
     */
    fun searchByRelationalCriteria(
        criteria: RelationalSearchCriteria, 
        pageable: Pageable
    ): Page<ConsultantWithCv>
    
    /**
     * Semantic search with quality-aware filtering
     * Combines vector similarity with cv_score.score_percent filtering
     */
    fun searchBySemanticCriteria(
        embedding: FloatArray, 
        criteria: SemanticSearchCriteria, 
        pageable: Pageable
    ): Page<ConsultantWithCv>
    
    /**
     * Finds CVs for a specific consultant with quality scores
     * Used by individual consultant detail views
     */
    fun findCvsWithQualityByUserId(userId: String, onlyActive: Boolean = false): List<Cv>
}
```

### 3. Search Criteria (Domain Layer) - Quality-Unified Criteria

**File**: `src/main/kotlin/no/cloudberries/candidatematch/domain/consultant/SearchCriteria.kt`

```kotlin
package no.cloudberries.candidatematch.domain.consultant

/**
 * Domain objects representing search criteria with business rules
 * Quality criteria always references cv_score.score_percent through CvQuality
 * Input validation ensures data integrity at domain boundaries
 */
data class RelationalSearchCriteria(
    val name: String? = null,
    val skillsAll: List<String> = emptyList(),
    val skillsAny: List<String> = emptyList(),
    /**
     * Minimum quality threshold based on cv_score.score_percent
     * When null, no quality filtering is applied
     * When present, filters WHERE COALESCE(cv_score.score_percent, 0) >= threshold
     */
    val minCvScorePercent: CvQuality? = null,
    val onlyActiveCv: Boolean = false
) {
    init {
        require(name?.isNotBlank() ?: true) { "Name filter cannot be blank" }
        require(skillsAll.all { it.isNotBlank() }) { "Skill names cannot be blank" }
        require(skillsAny.all { it.isNotBlank() }) { "Skill names cannot be blank" }
    }
    
    fun hasQualityFilter(): Boolean = minCvScorePercent != null
    fun hasSkillFilters(): Boolean = skillsAll.isNotEmpty() || skillsAny.isNotEmpty()
    
    /**
     * Gets the quality threshold for database queries
     * Returns the score_percent value to use in COALESCE expressions
     */
    fun getQualityThresholdForQuery(): Int? = minCvScorePercent?.scorePercent
}

data class SemanticSearchCriteria(
    val text: String,
    val topK: Int = 10,
    /**
     * Minimum quality threshold based on cv_score.score_percent
     * Applied after semantic similarity ranking but before final result set
     */
    val minCvScorePercent: CvQuality? = null,
    val onlyActiveCv: Boolean = false
) {
    init {
        require(text.isNotBlank()) { "Search text cannot be blank" }
        require(topK > 0) { "TopK must be positive" }
        require(topK <= 100) { "TopK cannot exceed 100" }
    }
    
    fun hasQualityFilter(): Boolean = minCvScorePercent != null
    
    /**
     * Gets the quality threshold for database queries
     * Returns the score_percent value to use in COALESCE expressions
     */
    fun getQualityThresholdForQuery(): Int? = minCvScorePercent?.scorePercent
}
```

### 4. Service Layer Implementation - Quality-Consistent Operations

**File**: `src/main/kotlin/no/cloudberries/candidatematch/service/consultant/ConsultantWithCvService.kt`

```kotlin
package no.cloudberries.candidatematch.service.consultant

import no.cloudberries.candidatematch.domain.consultant.*
import no.cloudberries.candidatematch.service.embedding.EmbeddingService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Application service orchestrating consultant search with unified CV quality
 * All operations ensure quality data is populated from cv_score.score_percent
 * Follows Single Responsibility Principle - handles search coordination only
 */
@Service
class ConsultantWithCvService(
    private val repository: ConsultantWithCvRepository,
    private val embeddingService: EmbeddingService
) {
    
    fun findAllWithCvQuality(onlyActive: Boolean = false): List<ConsultantWithCv> {
        logger.info { "Finding all consultants with CV quality from cv_score, onlyActive=$onlyActive" }
        return repository.findAllWithCvQuality(onlyActive)
    }
    
    fun findAllWithCvQualityPaged(
        onlyActive: Boolean = false, 
        pageable: Pageable
    ): Page<ConsultantWithCv> {
        logger.info { "Finding paged consultants with CV quality from cv_score, onlyActive=$onlyActive, page=${pageable.pageNumber}" }
        return repository.findAllWithCvQualityPaged(onlyActive, pageable)
    }
    
    fun findConsultantCvsWithQuality(userId: String, onlyActive: Boolean = false): List<Cv> {
        logger.info { "Finding CVs with quality for consultant $userId, onlyActive=$onlyActive" }
        return repository.findCvsWithQualityByUserId(userId, onlyActive)
    }
    
    fun searchRelational(
        criteria: RelationalSearchCriteria, 
        pageable: Pageable
    ): Page<ConsultantWithCv> {
        logger.info { 
            "Performing relational search with quality threshold: ${criteria.getQualityThresholdForQuery()}%"
        }
        return repository.searchByRelationalCriteria(criteria, pageable)
    }
    
    fun searchSemantic(
        criteria: SemanticSearchCriteria, 
        pageable: Pageable
    ): Page<ConsultantWithCv> {
        logger.info { 
            "Performing semantic search with quality threshold: ${criteria.getQualityThresholdForQuery()}%"
        }
        val embedding = embeddingService.embed(criteria.text)
        return repository.searchBySemanticCriteria(embedding, criteria, pageable)
    }
}
```

### 5. Infrastructure Implementation

**File**: `src/main/kotlin/no/cloudberries/candidatematch/infrastructure/repositories/JpaConsultantWithCvRepository.kt`

```kotlin
package no.cloudberries.candidatematch.infrastructure.repositories

import no.cloudberries.candidatematch.domain.consultant.*
import no.cloudberries.candidatematch.infrastructure.entities.ConsultantEntity
import no.cloudberries.candidatematch.infrastructure.mappers.ConsultantWithCvMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext

/**
 * Infrastructure implementation following Clean Architecture principles
 * Uses parameterized queries for security, efficient JOINs for performance
 */
interface JpaConsultantEntityRepository : JpaRepository<ConsultantEntity, Long> {
    
    @Query(value = """
        SELECT c.id, c.user_id, c.name, c.cv_id, c.resume_data,
               cv.id as cv_table_id, cv.active, cv.version_tag,
               cs.score_percent
        FROM consultant c 
        LEFT JOIN consultant_cv cv ON c.id = cv.consultant_id
        LEFT JOIN cv_score cs ON cv.id = cs.consultant_cv_id
        WHERE (:onlyActive = false OR cv.active = true)
        ORDER BY c.name ASC
    """, nativeQuery = true)
    fun findAllWithQualityScores(
        @Param("onlyActive") onlyActive: Boolean
    ): List<Array<Any>>
    
    @Query(value = """
        SELECT c.id, c.user_id, c.name, c.cv_id, c.resume_data,
               cv.id as cv_table_id, cv.active, cv.version_tag,
               cs.score_percent,
               COUNT(*) OVER() as total_count
        FROM consultant c 
        LEFT JOIN consultant_cv cv ON c.id = cv.consultant_id
        LEFT JOIN cv_score cs ON cv.id = cs.consultant_cv_id
        WHERE (:onlyActive = false OR cv.active = true)
        ORDER BY c.name ASC
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    fun findAllWithQualityScoresPaged(
        @Param("onlyActive") onlyActive: Boolean,
        @Param("limit") limit: Int,
        @Param("offset") offset: Long
    ): List<Array<Any>>
}

@Repository
class JpaConsultantWithCvRepository(
    private val jpaRepository: JpaConsultantEntityRepository,
    private val mapper: ConsultantWithCvMapper
) : ConsultantWithCvRepository {
    
    @PersistenceContext
    private lateinit var entityManager: EntityManager
    
    override fun findAllWithQuality(onlyActive: Boolean): List<ConsultantWithCv> {
        val rows = jpaRepository.findAllWithQualityScores(onlyActive)
        return mapper.mapRowsToConsultants(rows)
    }
    
    override fun findAllWithQualityPaged(onlyActive: Boolean, pageable: Pageable): Page<ConsultantWithCv> {
        val limit = pageable.pageSize
        val offset = pageable.offset
        
        val rows = jpaRepository.findAllWithQualityScoresPaged(onlyActive, limit, offset)
        val consultants = mapper.mapRowsToConsultants(rows)
        val totalCount = rows.firstOrNull()?.get(9)?.let { (it as Number).toLong() } ?: 0L
        
        return PageImpl(consultants, pageable, totalCount)
    }
    
    override fun searchByRelationalCriteria(
        criteria: RelationalSearchCriteria,
        pageable: Pageable
    ): Page<ConsultantWithCv> {
        val queryBuilder = StringBuilder("""
            SELECT c.id, c.user_id, c.name, c.cv_id, c.resume_data,
                   cv.id as cv_table_id, cv.active, cv.version_tag,
                   cs.score_percent,
                   COUNT(*) OVER() as total_count
            FROM consultant c 
            LEFT JOIN consultant_cv cv ON c.id = cv.consultant_id
            LEFT JOIN cv_score cs ON cv.id = cs.consultant_cv_id
            WHERE 1=1
        """)
        
        val params = mutableMapOf<String, Any?>()
        
        if (criteria.onlyActiveCv) {
            queryBuilder.append(" AND cv.active = true")
        }
        
        criteria.name?.let {
            queryBuilder.append(" AND LOWER(c.name) LIKE LOWER(:name)")
            params["name"] = "%$it%"
        }
        
        if (criteria.skillsAll.isNotEmpty()) {
            queryBuilder.append("""
                AND EXISTS (
                    SELECT 1 FROM consultant_skill csk 
                    JOIN skill sk ON csk.skill_id = sk.id
                    WHERE csk.consultant_id = c.id 
                    AND sk.name IN :skillsAll
                    GROUP BY csk.consultant_id 
                    HAVING COUNT(DISTINCT sk.name) = :skillsAllCount
                )
            """)
            params["skillsAll"] = criteria.skillsAll
            params["skillsAllCount"] = criteria.skillsAll.size
        }
        
        if (criteria.skillsAny.isNotEmpty()) {
            queryBuilder.append("""
                AND EXISTS (
                    SELECT 1 FROM consultant_skill csk 
                    JOIN skill sk ON csk.skill_id = sk.id
                    WHERE csk.consultant_id = c.id AND sk.name IN :skillsAny
                )
            """)
            params["skillsAny"] = criteria.skillsAny
        }
        
        criteria.minCvScorePercent?.let {
            queryBuilder.append(" AND COALESCE(cs.score_percent, 0) >= :minCvScorePercent")
            params["minCvScorePercent"] = it.scorePercent
        }
        
        queryBuilder.append(" ORDER BY c.name ASC LIMIT :limit OFFSET :offset")
        params["limit"] = pageable.pageSize
        params["offset"] = pageable.offset
        
        val query = entityManager.createNativeQuery(queryBuilder.toString())
        params.forEach { (key, value) -> query.setParameter(key, value) }
        
        @Suppress("UNCHECKED_CAST")
        val rows = query.resultList as List<Array<Any>>
        val consultants = mapper.mapRowsToConsultants(rows)
        val totalCount = rows.firstOrNull()?.get(9)?.let { (it as Number).toLong() } ?: 0L
        
        return PageImpl(consultants, pageable, totalCount)
    }
    
    override fun searchBySemanticCriteria(
        embedding: FloatArray,
        criteria: SemanticSearchCriteria,
        pageable: Pageable
    ): Page<ConsultantWithCv> {
        val embeddingStr = "[${embedding.joinToString(",")}]"
        
        val queryBuilder = StringBuilder("""
            SELECT c.id, c.user_id, c.name, c.cv_id, c.resume_data,
                   cv.id as cv_table_id, cv.active, cv.version_tag,
                   cs.score_percent,
                   (ce.embedding <-> :embedding::vector) as similarity_distance,
                   COUNT(*) OVER() as total_count
            FROM consultant c
            LEFT JOIN consultant_cv cv ON c.id = cv.consultant_id
            LEFT JOIN cv_score cs ON cv.id = cs.consultant_cv_id
            LEFT JOIN cv_embedding ce ON cv.id = ce.cv_id
            WHERE ce.embedding IS NOT NULL
        """)
        
        val params = mutableMapOf<String, Any?>(
            "embedding" to embeddingStr
        )
        
        if (criteria.onlyActiveCv) {
            queryBuilder.append(" AND cv.active = true")
        }
        
        criteria.minCvScorePercent?.let {
            queryBuilder.append(" AND COALESCE(cs.score_percent, 0) >= :minCvScorePercent")
            params["minCvScorePercent"] = it.scorePercent
        }
        
        queryBuilder.append(" ORDER BY similarity_distance ASC LIMIT :limit OFFSET :offset")
        params["limit"] = pageable.pageSize
        params["offset"] = pageable.offset
        
        val query = entityManager.createNativeQuery(queryBuilder.toString())
        params.forEach { (key, value) -> query.setParameter(key, value) }
        
        @Suppress("UNCHECKED_CAST")
        val rows = query.resultList as List<Array<Any>>
        val consultants = mapper.mapRowsToConsultants(rows)
        val totalCount = rows.firstOrNull()?.get(10)?.let { (it as Number).toLong() } ?: 0L
        
        return PageImpl(consultants, pageable, totalCount)
    }
}
```

### 6. Database Migration

**File**: `src/main/resources/db/migration/V999__add_cv_score_indexes.sql`

```sql
-- Add indexes for efficient CV quality score joins and filtering
-- Following database performance best practices

CREATE INDEX IF NOT EXISTS idx_cv_score_consultant_cv_id 
ON cv_score(consultant_cv_id);

CREATE INDEX IF NOT EXISTS idx_cv_score_score_percent 
ON cv_score(score_percent);

-- Composite index for common query patterns
CREATE INDEX IF NOT EXISTS idx_cv_score_consultant_score 
ON cv_score(consultant_cv_id, score_percent);

-- Index for consultant_cv active flag if not already present
CREATE INDEX IF NOT EXISTS idx_consultant_cv_active 
ON consultant_cv(active);
```

### 7. Controller Updates - Quality-Aware API Endpoints

**File**: Update existing `ConsultantController.kt`:

```kotlin
@RestController
@RequestMapping("/api/consultants")
class ConsultantController(
    private val consultantWithCvService: ConsultantWithCvService,
    private val consultantDtoMapper: ConsultantDtoMapper
) {
    
    @GetMapping("/with-cv")
    fun listConsultantsWithCv(
        @RequestParam(defaultValue = "false") onlyActiveCv: Boolean
    ): List<ConsultantWithCvDto> {
        // Service ensures all CVs have quality populated from cv_score.score_percent
        return consultantWithCvService.findAllWithCvQuality(onlyActiveCv)
            .map { consultantDtoMapper.toDto(it) }
    }
    
    @GetMapping("/with-cv/paged")
    fun listConsultantsWithCvPaged(
        @RequestParam(defaultValue = "false") onlyActiveCv: Boolean,
        pageable: Pageable
    ): Page<ConsultantWithCvDto> {
        // Service ensures all CVs have quality populated from cv_score.score_percent
        return consultantWithCvService.findAllWithCvQualityPaged(onlyActiveCv, pageable)
            .map { consultantDtoMapper.toDto(it) }
    }
    
    @GetMapping("/{userId}/cvs")
    fun getConsultantCvs(
        @PathVariable userId: String,
        @RequestParam(defaultValue = "false") onlyActiveCv: Boolean
    ): List<ConsultantCvDto> {
        // Service ensures CVs include quality from cv_score.score_percent
        return consultantWithCvService.findConsultantCvsWithQuality(userId, onlyActiveCv)
            .map { consultantDtoMapper.toCvDto(it) }
    }
    
    @PostMapping("/search")
    fun searchRelational(
        @RequestBody @Valid request: RelationalSearchRequest,
        pageable: Pageable
    ): Page<ConsultantWithCvDto> {
        // API minQualityScore maps to cv_score.score_percent filtering
        val criteria = consultantDtoMapper.toDomainCriteria(request)
        return consultantWithCvService.searchRelational(criteria, pageable)
            .map { consultantDtoMapper.toDto(it) }
    }
    
    @PostMapping("/search/semantic")
    fun searchSemantic(
        @RequestBody @Valid request: SemanticSearchRequest,
        pageable: Pageable
    ): Page<ConsultantWithCvDto> {
        // API minQualityScore maps to cv_score.score_percent filtering
        val criteria = consultantDtoMapper.toDomainCriteria(request)
        return consultantWithCvService.searchSemantic(criteria, pageable)
            .map { consultantDtoMapper.toDto(it) }
    }
}
```

### 8. DTO Mapping - API Compatibility with Domain Coherence

**File**: `src/main/kotlin/no/cloudberries/candidatematch/infrastructure/mappers/ConsultantDtoMapper.kt`

```kotlin
@Component
class ConsultantDtoMapper {
    
    /**
     * Maps API search request to domain criteria
     * API minQualityScore maps to domain CvQuality which references cv_score.score_percent
     */
    fun toDomainCriteria(request: RelationalSearchRequest): RelationalSearchCriteria {
        return RelationalSearchCriteria(
            name = request.name?.takeIf { it.isNotBlank() },
            skillsAll = request.skillsAll?.filterNot { it.isBlank() } ?: emptyList(),
            skillsAny = request.skillsAny?.filterNot { it.isBlank() } ?: emptyList(),
            // API compatibility: minQualityScore -> domain CvQuality -> cv_score.score_percent
            minCvScorePercent = request.minQualityScore?.let { CvQuality.fromApiScore(it) },
            onlyActiveCv = request.onlyActiveCv
        )
    }
    
    /**
     * Maps semantic search API request to domain criteria
     * Maintains API compatibility while using unified quality concept
     */
    fun toDomainCriteria(request: SemanticSearchRequest): SemanticSearchCriteria {
        return SemanticSearchCriteria(
            text = request.text,
            topK = request.topK,
            // API compatibility: minQualityScore -> domain CvQuality -> cv_score.score_percent
            minCvScorePercent = request.minQualityScore?.let { CvQuality.fromApiScore(it) },
            onlyActiveCv = request.onlyActiveCv
        )
    }
    
    /**
     * Maps domain consultant to DTO for API response
     * Preserves API structure while using unified domain quality concept
     */
    fun toDto(domain: ConsultantWithCv): ConsultantWithCvDto {
        return ConsultantWithCvDto(
            id = domain.id,
            userId = domain.userId,
            name = domain.name,
            cvId = domain.defaultCvId,
            skills = domain.topSkills.take(3),
            cvs = domain.cvs.map { cv ->
                ConsultantCvDto(
                    id = cv.id.toLongOrNull(),
                    versionTag = cv.versionTag,
                    // API compatibility: domain CvQuality -> DTO qualityScore
                    // This value comes from cv_score.score_percent in the domain
                    qualityScore = cv.quality?.toApiScore(),
                    active = cv.active,
                    // ... map other CV fields following existing patterns
                )
            }
        )
    }
    
    /**
     * Maps individual CV from domain to DTO
     * Used for consultant detail views and CV-specific endpoints
     */
    fun toCvDto(domain: Cv): ConsultantCvDto {
        return ConsultantCvDto(
            id = domain.id.toLongOrNull(),
            versionTag = domain.versionTag,
            // Quality score sourced from cv_score.score_percent
            qualityScore = domain.quality?.toApiScore(),
            active = domain.active,
            // ... map other fields
        )
    }
}
```

### 9. Testing Strategy

**File**: `src/test/kotlin/no/cloudberries/candidatematch/service/ConsultantWithCvServiceIT.kt`

```kotlin
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ConsultantWithCvServiceIT {
    
    @Autowired
    lateinit var service: ConsultantWithCvService
    
    @Autowired
    lateinit var testDataBuilder: TestDataBuilder
    
    @Test
    fun `should populate CV quality from cv_score table score_percent`() {
        // Given: consultant with CV and cv_score.score_percent entry
        val consultant = testDataBuilder.createConsultant("John Doe")
        val cv = testDataBuilder.createCv(consultant.id, active = true)
        testDataBuilder.createCvScore(cv.id, scorePercent = 85)
        
        // When
        val result = service.findAllWithCvQuality(onlyActive = true)
        
        // Then: CvQuality should be populated from cv_score.score_percent
        assertThat(result).hasSize(1)
        val consultantWithCv = result.first()
        assertThat(consultantWithCv.cvs).hasSize(1)
        assertThat(consultantWithCv.cvs.first().quality?.scorePercent).isEqualTo(85)
        assertThat(consultantWithCv.cvs.first().quality?.asPercentage).isEqualTo(85)
    }
    
    @Test
    fun `should return null quality when no cv_score entry exists`() {
        // Given: consultant with CV but no cv_score.score_percent entry
        val consultant = testDataBuilder.createConsultant("Jane Doe")
        val cv = testDataBuilder.createCv(consultant.id, active = true)
        // No cv_score entry created - LEFT JOIN will return null
        
        // When
        val result = service.findAllWithCvQuality(onlyActive = true)
        
        // Then: Quality should be null when no cv_score.score_percent exists
        assertThat(result).hasSize(1)
        val consultantWithCv = result.first()
        assertThat(consultantWithCv.cvs).hasSize(1)
        assertThat(consultantWithCv.cvs.first().quality).isNull()
    }
    
    @Test
    fun `should filter by minCvScorePercent in relational search`() {
        // Given: consultants with different cv_score.score_percent values
        val lowQualityConsultant = testDataBuilder.createConsultantWithCvScore("Low Quality", scorePercent = 30)
        val highQualityConsultant = testDataBuilder.createConsultantWithCvScore("High Quality", scorePercent = 90)
        
        val criteria = RelationalSearchCriteria(
            minCvScorePercent = CvQuality.fromScorePercent(60)
        )
        
        // When
        val result = service.searchRelational(criteria, Pageable.unpaged())
        
        // Then: only consultant with cv_score.score_percent >= 60 should be returned
        assertThat(result.content).hasSize(1)
        assertThat(result.content.first().name).isEqualTo("High Quality")
        assertThat(result.content.first().cvs.first().quality?.scorePercent).isEqualTo(90)
    }
    
    @Test
    fun `should treat missing cv_score as zero when filtering`() {
        // Given: consultants with and without cv_score.score_percent values
        val noScoreConsultant = testDataBuilder.createConsultantWithoutCvScore("No Score")
        val withScoreConsultant = testDataBuilder.createConsultantWithCvScore("With Score", scorePercent = 70)
        
        val criteria = RelationalSearchCriteria(
            minCvScorePercent = CvQuality.fromScorePercent(50)
        )
        
        // When
        val result = service.searchRelational(criteria, Pageable.unpaged())
        
        // Then: only consultant with cv_score.score_percent >= 50 should be returned
        // Missing cv_score entries are treated as 0 via COALESCE
        assertThat(result.content).hasSize(1)
        assertThat(result.content.first().name).isEqualTo("With Score")
        assertThat(result.content.first().cvs.first().quality?.scorePercent).isEqualTo(70)
    }
    
    @Test
    fun `should handle semantic search with cv_score filtering`() {
        // Given: consultants with embeddings and cv_score.score_percent values
        val consultant1 = testDataBuilder.createConsultantWithCvScoreAndEmbedding(
            name = "Kotlin Expert",
            scorePercent = 90,
            skills = listOf("Kotlin", "Spring")
        )
        val consultant2 = testDataBuilder.createConsultantWithCvScoreAndEmbedding(
            name = "Java Expert", 
            scorePercent = 30,
            skills = listOf("Java", "Spring")
        )
        
        val criteria = SemanticSearchCriteria(
            text = "Kotlin developer",
            topK = 10,
            minCvScorePercent = CvQuality.fromScorePercent(50)
        )
        
        // When
        val result = service.searchSemantic(criteria, Pageable.unpaged())
        
        // Then: only consultant with cv_score.score_percent >= 50 should be returned
        assertThat(result.content).hasSize(1)
        assertThat(result.content.first().name).isEqualTo("Kotlin Expert")
        assertThat(result.content.first().cvs.first().quality?.scorePercent).isEqualTo(90)
    }
}
```

### 10. OpenAPI Updates

**File**: `candidate-match/openapi.yaml`

```yaml
components:
  schemas:
    ConsultantCvDto:
      type: object
      properties:
        qualityScore:
          type: integer
          minimum: 0
          maximum: 100
          nullable: true
          description: "Integer 0–100; populated from cv_score.score_percent. Null if no quality score available."
          example: 85
          
    RelationalSearchRequest:
      type: object
      properties:
        minQualityScore:
          type: integer
          minimum: 0
          maximum: 100
          nullable: true
          description: "Minimum CV quality score (0–100). Candidates without a score are treated as 0 for filtering."
          example: 70
          
    SemanticSearchRequest:
      type: object
      properties:
        minQualityScore:
          type: integer
          minimum: 0
          maximum: 100
          nullable: true
          description: "Minimum CV quality score (0–100). Candidates without a score are treated as 0 for filtering."
          example: 60
```

## Implementation Order

1. **Domain Model** - Create `CvQuality` value object and update domain entities
2. **Repository Interface** - Define clean contracts in domain layer  
3. **Database Migration** - Add necessary indexes for performance
4. **Infrastructure** - Implement repository with efficient joins and parameterized queries
5. **Service Layer** - Wire up search and enrichment logic with proper error handling
6. **Controller Updates** - Update existing endpoints with input validation
7. **Testing** - Add comprehensive integration tests with test containers
8. **OpenAPI** - Update documentation and sync to frontend

## Key Principles Followed

1. **Single Source of Truth**: `cv_score.score_percent` is the authoritative quality score across all layers
2. **Domain Coherence**: All quality-related operations reference the same underlying cv_score concept
3. **Single Responsibility Principle**: Each component has one clear purpose
4. **Dependency Inversion Principle**: Domain defines quality contracts, infrastructure implements
5. **Domain-Driven Design**: Rich value objects (`CvQuality`) encapsulate cv_score business rules
6. **Clean Architecture**: Clear separation with unified domain concepts across layers
7. **API Compatibility**: DTOs maintain existing structure while mapping to unified domain model
8. **Performance**: Efficient JOINs with cv_score table, proper indexing, avoid N+1 problems
9. **Security**: Parameterized queries prevent SQL injection
10. **Testability**: Clear boundaries enable isolated unit and integration testing

## Performance Considerations

- **LEFT JOIN** ensures no data loss when quality scores are missing
- **Composite indexes** on frequently queried columns for optimal performance
- **Native SQL** for complex queries to avoid ORM overhead and N+1 problems
- **Pagination** at database level to handle large result sets efficiently
- **COALESCE** function treats missing scores as 0 for consistent filtering behavior

## Security Considerations

- **Parameterized queries** prevent SQL injection attacks
- **Input validation** in domain objects (CvQuality constructor, SearchCriteria init blocks)
- **Read-only operations** for search endpoints (principle of least privilege)
- **No sensitive data** logged or exposed in error messages

## Rollback Strategy

If issues arise:
1. **Disable quality filtering** by ignoring `minQualityScore` parameters in service layer
2. **Return null** for `qualityScore` in DTOs (frontend handles gracefully with fallback)
3. **Database rollback** via migration rollback if indexes cause performance issues
4. **Feature toggle** to switch between old and new behavior during transition

## Development Environment Setup

Before starting implementation:

```bash
# Use SDKMAN for Java and Maven
sdk install java 21.0.7-tem
sdk use java 21.0.7-tem
sdk install maven

# Add .sdkmanrc to project root
echo "java=21.0.7-tem" > .sdkmanrc
echo "maven=3.9.6" >> .sdkmanrc

# Enable auto-switching
sdk env install
sdk env

# Start local database
docker-compose -f docker-compose-local.yaml up -d

# Verify database connection with username/password auth only
```

## Post-Implementation Tasks

After backend changes are complete:

```bash
# Copy updated OpenAPI to frontend
cp ~/git/cloudberries-candidate-match/candidate-match/openapi.yaml ~/git/cloudberries-candidate-match-web/openapi.yaml

# Regenerate frontend types
cd ~/git/cloudberries-candidate-match-web
npm run gen:api

# Verify build
npm run build

# Remove frontend fallback after verification
# Set VITE_ENABLE_CV_SCORE_FALLBACK=false or remove fallback code
```