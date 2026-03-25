# Implementation Plan: Batch Matching with Files API and Model Fallback

**Date:** 2025-11-23  
**Author:** WARP AI Agent  
**Project:** cloudberries-candidate-match  
**Module:** candidate-match

---

## Executive Summary

This document provides step-by-step instructions to refactor the candidate matching system to:
1. **Enable batch matching** using Gemini Files API (`GeminiFilesMatchingService.matchConsultantsWithFilesApi`)
2. **Implement fallback logic** to switch from `gemini-3-pro-preview` to `gemini-2.5-pro` on 503 overload errors

---

## Problem Statement

### Current State Analysis

From the stack trace and code review, the current flow is:

```
ProjectRequestController.uploadAndAnalyze()
  ↓
projectMatchingService.triggerAsyncMatching(projectRequestId)
  ↓
ProjectMatchingServiceImpl.computeAndPersistMatches()
  ↓
computeMatchesUsingGemini()
  ↓
geminiMatchingStrategy.computeMatches()
  ↓
geminiMatchingPort.rankCandidates()  ← Uses GeminiFileSearchAdapter (legacy)
```

**Issues:**
1. `GeminiFilesMatchingService.matchConsultantsWithFilesApi()` is **never called**
2. The system uses sequential AI calls per candidate instead of batch ranking
3. No fallback mechanism when `gemini-3-pro-preview` returns 503 overload errors
4. Two separate matching implementations exist but the wrong one is wired

### Desired State

```
ProjectRequestController.uploadAndAnalyze()
  ↓
projectMatchingService.triggerAsyncMatching(projectRequestId)
  ↓
ProjectMatchingServiceImpl.computeAndPersistMatches()
  ↓
GeminiFilesMatchingService.matchConsultantsWithFilesApi()  ← New entry point
  ↓
GeminiFilesApiAdapter.rankCandidatesWithFiles()
  ↓ (on 503 error)
Retry with gemini-2.5-pro ← Fallback
```

---

## Implementation Steps

### Step 1: Wire `GeminiFilesMatchingService` into `ProjectMatchingServiceImpl`

**File:** `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/matches/service/ProjectMatchingServiceImpl.kt`

**Current code (lines 36-45):**
```kotlin
@Service
class ProjectMatchingServiceImpl(
    private val projectMatchResultRepository: ProjectMatchResultRepository,
    private val matchCandidateResultRepository: MatchCandidateResultRepository,
    private val projectRequestRepository: ProjectRequestRepository,
    private val consultantWithCvService: ConsultantWithCvService,
    private val candidateMatchingService: CandidateMatchingService,
    private val consultantScoringService: ConsultantScoringService,
    private val geminiMatchingStrategy: GeminiMatchingStrategy?
) : ProjectMatchingService {
```

**Change 1.1:** Add `GeminiFilesMatchingService` as dependency

```kotlin
@Service
class ProjectMatchingServiceImpl(
    private val projectMatchResultRepository: ProjectMatchResultRepository,
    private val matchCandidateResultRepository: MatchCandidateResultRepository,
    private val projectRequestRepository: ProjectRequestRepository,
    private val consultantWithCvService: ConsultantWithCvService,
    private val candidateMatchingService: CandidateMatchingService,
    private val consultantScoringService: ConsultantScoringService,
    private val geminiMatchingStrategy: GeminiMatchingStrategy?,
    private val geminiFilesMatchingService: GeminiFilesMatchingService  // NEW
) : ProjectMatchingService {
```

**Change 1.2:** Update `computeAndPersistMatches()` to use Files API (lines 99-106)

**Current code:**
```kotlin
// Compute matches using Gemini strategy if available, otherwise use legacy approach
val candidateResults = if (geminiMatchingStrategy != null) {
    logger.info { "Using Gemini File Search matching strategy" }
    computeMatchesUsingGemini(projectRequest, savedMatchResult)
} else {
    logger.info { "Using legacy AI matching approach" }
    computeMatchesInParallel(consultants, projectRequest, savedMatchResult)
}
```

**Replace with:**
```kotlin
// Use Gemini Files API for batch matching
val candidateResults = try {
    logger.info { "Using Gemini Files API batch matching" }
    computeMatchesUsingFilesApi(projectRequest, savedMatchResult)
} catch (e: Exception) {
    logger.error(e) { "Gemini Files API failed, falling back to legacy matching" }
    computeMatchesInParallel(consultants, projectRequest, savedMatchResult)
}
```

**Change 1.3:** Add new method `computeMatchesUsingFilesApi()` after line 176

```kotlin
/**
 * Computes matches using Gemini Files API batch approach.
 * This calls GeminiFilesMatchingService which:
 * 1. Uploads CVs to Gemini Files API (with caching)
 * 2. Ranks all candidates in a SINGLE Gemini API call
 * 3. Returns sorted list with scores and justifications
 */
private suspend fun computeMatchesUsingFilesApi(
    projectRequestEntity: no.cloudberries.candidatematch.infrastructure.entities.ProjectRequestEntity,
    matchResult: ProjectMatchResult
): List<MatchCandidateResult> {
    // Convert entity to domain model
    val projectRequest = convertToDomainProjectRequest(projectRequestEntity)
    
    // Extract required skills for filtering
    val requiredSkills = projectRequest.requiredSkills.map { it.name }
    
    // Call batch matching service
    val matchedConsultants = geminiFilesMatchingService.matchConsultantsWithFilesApi(
        projectRequest = projectRequest,
        requiredSkills = requiredSkills,
        topN = 10
    )
    
    // Convert MatchConsultantDto to MatchCandidateResult entities
    return matchedConsultants.mapNotNull { dto ->
        val consultantId = dto.userId.toLongOrNull()
        if (consultantId == null) {
            logger.warn { "Invalid consultant ID in Files API result: ${dto.userId}" }
            return@mapNotNull null
        }
        
        // Convert 0-100 score to 0.0-1.0 decimal
        val scoreDecimal = BigDecimal.valueOf(dto.relevanceScore).divide(BigDecimal.valueOf(100))
            .coerceIn(BigDecimal.ZERO, BigDecimal.ONE)
        
        MatchCandidateResult(
            matchResult = matchResult,
            consultantId = consultantId,
            matchScore = scoreDecimal,
            matchExplanation = dto.justification ?: "Ranked by Gemini Files API"
        )
    }
}
```

---

### Step 2: Implement Fallback Logic in `GeminiFilesApiAdapter`

**File:** `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/infrastructure/gemini/GeminiFilesApiAdapter.kt`

**Change 2.1:** Add retry logic with model fallback in `rankCandidatesWithFiles()` (lines 101-162)

**Current code:**
```kotlin
try {
    // Use matchingModel for batch ranking, not the main model (which is for PDF analysis)
    val model = geminiProperties.matchingModel
    val response: Map<String, Any> = geminiWebClient.post()
        .uri("/v1beta/models/$model:generateContent?key=${geminiProperties.apiKey}")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .awaitBody()

    val textResponse = extractTextFromResponse(response)
    val parsed = objectMapper.readValue(textResponse, RankedCandidatesResult::class.java)
        .copy(projectRequestId = projectRequestId)

    log.info { "Successfully ranked ${parsed.ranked.size} candidates for project $projectRequestId" }
    return parsed.copy(ranked = parsed.ranked.take(topN))
    
} catch (e: Exception) {
    log.error(e) { "Failed to rank candidates with Files API for project $projectRequestId" }
    // Graceful degradation - return empty list instead of crashing
    return RankedCandidatesResult(projectRequestId = projectRequestId, ranked = emptyList())
}
```

**Replace with:**
```kotlin
// Try with primary matching model first (gemini-2.5-pro)
var model = geminiProperties.matchingModel
var attempt = 1
val maxAttempts = 2

while (attempt <= maxAttempts) {
    try {
        log.info { "Ranking attempt $attempt/$maxAttempts with model: $model" }
        
        val response: Map<String, Any> = geminiWebClient.post()
            .uri("/v1beta/models/$model:generateContent?key=${geminiProperties.apiKey}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .awaitBody()

        val textResponse = extractTextFromResponse(response)
        val parsed = objectMapper.readValue(textResponse, RankedCandidatesResult::class.java)
            .copy(projectRequestId = projectRequestId)

        log.info { "Successfully ranked ${parsed.ranked.size} candidates with $model" }
        return parsed.copy(ranked = parsed.ranked.take(topN))
        
    } catch (e: Exception) {
        val errorMessage = e.message ?: ""
        val is503Overload = errorMessage.contains("503") || 
                           errorMessage.contains("overloaded") || 
                           errorMessage.contains("ServerException")
        
        if (is503Overload && attempt == 1) {
            // Fallback to flashModel (gemini-2.5-flash) on first 503
            log.warn { "Model $model overloaded (503), falling back to ${geminiProperties.flashModel}" }
            model = geminiProperties.flashModel
            attempt++
            kotlinx.coroutines.delay(1000) // Wait 1 second before retry
            continue
        } else {
            log.error(e) { "Failed to rank with $model after $attempt attempts" }
            // Graceful degradation - return empty list
            return RankedCandidatesResult(projectRequestId = projectRequestId, ranked = emptyList())
        }
    }
}

// Should not reach here, but safety fallback
return RankedCandidatesResult(projectRequestId = projectRequestId, ranked = emptyList())
```

**Note:** This approach tries `gemini-2.5-pro` first, and falls back to `gemini-2.5-flash` if 503 overload occurs.

---

### Step 3: Update Configuration Comments for Clarity

**File:** `candidate-match/src/main/resources/application-local.yaml`

**Change 3.1:** Update Gemini config comments (lines 44-60)

```yaml
gemini:
  apiKey: ${GEMINI_API_KEY}
  
  # Model for project analysis (PDF upload, Må/Bør-krav extraction)
  # gemini-3-pro-preview gives best quality but has capacity limits
  # For PDF analysis, this model is used
  model: gemini-3-pro-preview
  
  # Model for candidate ranking (batch operations via Files API)
  # PRIMARY: gemini-2.5-pro - stable, 2M context, high rate limits
  # This is used as the first choice for batch ranking
  matchingModel: gemini-2.5-pro
  
  # FALLBACK: gemini-2.5-flash - fast, used if matchingModel returns 503
  flashModel: gemini-2.5-flash
  
  fileStoreName: candidate-cv-store
  useFilesApi: true  # MUST be true for Files API batch matching
```

---

### Step 4: Remove Legacy Conditional Bean (Optional Cleanup)

**Background:** `GeminiMatchingStrategy` is annotated with `@ConditionalOnProperty(prefix = "matching", name = ["provider"], havingValue = "GEMINI")` which creates it as a bean only when `matching.provider=GEMINI`. Since we're now calling `GeminiFilesMatchingService` directly, we can optionally keep this for backwards compatibility or remove it.

**Recommended approach:** Keep it for now to avoid breaking changes, but mark it as deprecated.

**File:** `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/matches/service/GeminiMatchingStrategy.kt`

**Change 4.1:** Add deprecation annotation (after line 28)

```kotlin
/**
 * Gemini File Search-based matching strategy.
 * Uses Gemini's managed RAG to rank candidates against project requirements.
 * 
 * Activated when matching.provider=GEMINI in application.yaml
 * 
 * @deprecated This strategy is being replaced by GeminiFilesMatchingService
 * which uses the Files API for batch matching. Kept for backwards compatibility.
 */
@Deprecated(
    message = "Use GeminiFilesMatchingService for Files API batch matching",
    replaceWith = ReplaceWith("GeminiFilesMatchingService")
)
@Service
@ConditionalOnProperty(prefix = "matching", name = ["provider"], havingValue = "GEMINI")
class GeminiMatchingStrategy(
```

---

### Step 5: Add Logging to Track Execution Path

**File:** `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/controllers/projectrequest/ProjectRequestController.kt`

**Change 5.1:** Improve logging at line 63

**Current:**
```kotlin
if (createdId != null) {
    projectMatchingService.triggerAsyncMatching(createdId, forceRecompute = false)
}
```

**Replace with:**
```kotlin
if (createdId != null) {
    logger.info { "Triggering async batch matching for project request $createdId" }
    projectMatchingService.triggerAsyncMatching(createdId, forceRecompute = false)
} else {
    logger.warn { "Failed to create project request entity, skipping matching" }
}
```

---

## Testing Strategy

### Unit Tests

1. **Test `computeMatchesUsingFilesApi()` in `ProjectMatchingServiceImpl`**
   - Mock `GeminiFilesMatchingService.matchConsultantsWithFilesApi()`
   - Verify correct conversion from `MatchConsultantDto` to `MatchCandidateResult`
   - Verify score conversion (0-100 → 0.0-1.0)

2. **Test fallback logic in `GeminiFilesApiAdapter`**
   - Mock `geminiWebClient` to throw 503 exception on first call
   - Verify retry with `flashModel`
   - Verify graceful degradation to empty list after max attempts

### Integration Tests

1. **End-to-end matching flow**
   - Upload a test PDF project request
   - Verify `GeminiFilesMatchingService.matchConsultantsWithFilesApi()` is called
   - Verify matches are persisted in database
   - Verify correct logging output

2. **503 Fallback scenario**
   - Simulate 503 from Gemini API
   - Verify automatic retry with fallback model
   - Verify final results are still persisted

### Manual Testing

1. **Run with local profile:**
   ```bash
   mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local
   ```

2. **Upload project request via Swagger UI:**
   - Go to `http://localhost:8080/swagger-ui/index.html`
   - POST to `/api/project-requests/upload`
   - Upload a PDF (e.g., "Politiet_Fullstackutvikler.pdf")

3. **Check logs for:**
   ```
   [FILES API] Starting match for project X (topN=5)
   [STEP 1] Fetching candidate pool with N required skills
   [STEP 2] Scoring consultants by 50% skills + 50% CV quality
   [STEP 3] Uploading CVs to Gemini Files API (with caching)
   [STEP 4] Calling Gemini API with N file references in SINGLE request
   [RESULT] Returning N matched consultants
   ```

4. **Verify database:**
   ```sql
   SELECT COUNT(*) FROM match_candidate_result WHERE match_result_id = X;
   SELECT consultant_id, match_score, match_explanation 
   FROM match_candidate_result 
   WHERE match_result_id = X 
   ORDER BY match_score DESC;
   ```

---

## Rollback Plan

If the implementation causes issues:

1. **Revert Step 1 changes** in `ProjectMatchingServiceImpl.kt`:
   - Remove `geminiFilesMatchingService` dependency
   - Restore original `computeAndPersistMatches()` logic

2. **Keep Step 2 changes** (fallback logic) as they improve resilience

3. **Re-enable legacy flow** by setting in `application-local.yaml`:
   ```yaml
   gemini:
     useFilesApi: false
   ```

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Files API returns 404 for cached URIs after 48h | Medium | Medium | Current code already handles cache invalidation |
| 503 overload on both models | Low | High | Graceful degradation returns empty list, manual retry possible |
| Score conversion bug (0-100 vs 0.0-1.0) | Low | Medium | Unit test coverage + validation in code |
| Consultant ID parsing failure | Low | Low | Null check with warning log |
| Database constraint violation | Very Low | Medium | Transaction rollback + error logging |

---

## Success Criteria

✅ Upload project request triggers `GeminiFilesMatchingService.matchConsultantsWithFilesApi()`  
✅ Logs show "Using Gemini Files API batch matching"  
✅ Single Gemini API call ranks all candidates (not N sequential calls)  
✅ Matches are persisted to database with scores 0.0-1.0  
✅ 503 errors trigger fallback to `gemini-2.5-flash`  
✅ No 404 errors for Gemini models  
✅ End-to-end matching completes in < 30 seconds (vs > 5 minutes before)  

---

## Implementation Checklist

- [ ] Step 1.1: Add `GeminiFilesMatchingService` dependency to `ProjectMatchingServiceImpl`
- [ ] Step 1.2: Update `computeAndPersistMatches()` to call Files API
- [ ] Step 1.3: Add `computeMatchesUsingFilesApi()` method
- [ ] Step 2.1: Add retry and fallback logic to `GeminiFilesApiAdapter.rankCandidatesWithFiles()`
- [ ] Step 3.1: Update configuration comments in `application-local.yaml`
- [ ] Step 4.1: Add deprecation annotation to `GeminiMatchingStrategy` (optional)
- [ ] Step 5.1: Improve logging in `ProjectRequestController`
- [ ] Write unit tests for new methods
- [ ] Write integration test for end-to-end flow
- [ ] Manual testing with PDF upload
- [ ] Database verification
- [ ] Document findings and any issues

---

## Additional Considerations

### Performance Optimization

After implementation, monitor:
- Gemini API response times (target: < 10s for 10 candidates)
- CV upload caching hit rate (target: > 70%)
- Database query performance for candidate selection
- Memory usage during batch operations

### Future Enhancements

1. **Parallel file uploads:** Currently uploads CVs sequentially. Could batch upload in parallel for faster preparation.
2. **Smart cache invalidation:** Track file expiry (48h) and proactively re-upload before expiry.
3. **Adaptive topN:** Dynamically adjust candidate pool size based on project complexity.
4. **Telemetry:** Add metrics for matching duration, success rate, fallback frequency.

---

## References

- [Gemini Files API Documentation](https://ai.google.dev/api/files)
- [Gemini Long Context Guide](https://ai.google.dev/gemini-api/docs/long-context)
- Project WARP.md: `/Users/tandersen/git/cloudberries-candidate-match/WARP.md`
- Previous implementation doc: `GEMINI_FILES_API_IMPLEMENTATION.md`

---

**End of Implementation Plan**
