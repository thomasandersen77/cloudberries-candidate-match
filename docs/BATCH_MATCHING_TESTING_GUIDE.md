# Batch Matching Testing Guide

**Date:** 2025-11-23  
**Implementation:** Gemini Files API Batch Matching with Fallback

---

## Overview

This guide explains how to test the new batch matching implementation that uses Gemini Files API instead of sequential AI calls.

## What Changed

### Before (Sequential Matching)
```
For each consultant:
  1. Build CV text
  2. Call Gemini API
  3. Parse response
  4. Save to database
→ N API calls for N consultants (slow, expensive)
```

### After (Batch Matching)
```
1. Select top 10 candidates (SQL + scoring)
2. Upload CVs to Gemini Files API (with caching)
3. Single Gemini API call with all file references
4. Parse batch response
5. Save all results to database
→ 1 API call for 10 consultants (fast, cheap)
```

## Prerequisites

1. **Database running:**
   ```bash
   cd candidate-match
   docker-compose -f docker-compose-local.yaml up -d
   ```

2. **Environment variables set:**
   ```bash
   export GEMINI_API_KEY="your-api-key-here"
   export FLOWCASE_API_KEY="optional-for-testing"
   export FLOWCASE_BASE_URL="https://cloudberries.flowcase.com/api"
   ```

3. **Application compiled:**
   ```bash
   mvn clean compile
   ```

## Testing Methods

### Method 1: Manual Integration Test (Recommended)

**Location:** `src/test/kotlin/no/cloudberries/candidatematch/matches/service/ProjectMatchingBatchIntegrationTest.kt`

**How to run:**
1. Open the test file in IntelliJ IDEA
2. Remove or comment out the `@Disabled` annotation
3. Right-click on `testBatchMatchingWithFilesApi` method
4. Select "Run 'testBatchMatchingWithFilesApi()'"

**What it tests:**
- Direct call to `GeminiFilesMatchingService.matchConsultantsWithFilesApi()`
- CV upload to Gemini (with caching detection)
- Single batch ranking call
- Fallback to gemini-2.5-flash if gemini-2.5-pro returns 503
- Result parsing and score validation

**Expected log output:**
```
=== Starting Manual Batch Matching Integration Test ===
Project Request: Test Customer - Politiet
Required Skills: Kotlin, Spring Boot, PostgreSQL, React, TypeScript

=== Calling GeminiFilesMatchingService.matchConsultantsWithFilesApi() ===
[FILES API] Starting match for project 999 (topN=5)
[STEP 1] Fetching candidate pool with 5 required skills
[STEP 1] Retrieved 30 consultants from database
[STEP 2] Scoring consultants by 50% skills + 50% CV quality
[STEP 2] Selected 10 consultants for Gemini evaluation
[STEP 3] Uploading CVs to Gemini Files API (with caching)
Using cached file URI for John Doe
Successfully uploaded CV for Jane Smith -> files/abc123xyz
[STEP 4] Calling Gemini API with 10 file references in SINGLE request
Ranking attempt 1/2 with model: gemini-2.5-pro
Successfully ranked 5 candidates with gemini-2.5-pro
[RESULT] Returning 5 matched consultants

=== Batch Matching Completed Successfully ===
Number of matches returned: 5

Rank 1:
  Name: John Doe
  Score: 92/100
  Justification: Har omfattende erfaring med Kotlin, Spring Boot og React...
```

### Method 2: End-to-End via API

**Step 1:** Start the application
```bash
cd candidate-match
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Step 2:** Open Swagger UI
```
http://localhost:8080/swagger-ui/index.html
```

**Step 3:** Upload a project request PDF
- Navigate to POST `/api/project-requests/upload`
- Click "Try it out"
- Upload a PDF file (e.g., `Politiet_Fullstackutvikler.pdf`)
- Click "Execute"

**Step 4:** Monitor application logs
Watch for these log messages:
```
ProjectRequestController: Triggering async batch matching for project request 42
ProjectMatchingServiceImpl: Using Gemini Files API batch matching
[FILES API] Starting match for project 42 (topN=5)
[STEP 1] Fetching candidate pool...
[STEP 2] Scoring consultants...
[STEP 3] Uploading CVs to Gemini...
[STEP 4] Calling Gemini API with 10 file references in SINGLE request
Ranking attempt 1/2 with model: gemini-2.5-pro
Successfully ranked 5 candidates with gemini-2.5-pro
[RESULT] Returning 5 matched consultants
```

**Step 5:** Query database to verify results
```sql
-- Find the latest match result
SELECT * FROM project_match_result 
WHERE project_request_id = 42 
ORDER BY created_at DESC 
LIMIT 1;

-- Get candidate matches
SELECT 
  c.name,
  mcr.match_score,
  mcr.match_explanation,
  mcr.created_at
FROM match_candidate_result mcr
JOIN consultant c ON c.id = mcr.consultant_id
WHERE mcr.match_result_id = (
  SELECT id FROM project_match_result 
  WHERE project_request_id = 42 
  ORDER BY created_at DESC 
  LIMIT 1
)
ORDER BY mcr.match_score DESC;
```

**Step 6:** Retrieve via API
```bash
# Get match results
curl http://localhost:8080/api/matches/42?limit=10 | jq

# Check status
curl http://localhost:8080/api/matches/status/42 | jq
```

### Method 3: Direct Database Verification

**Check if batch matching was used:**
```sql
-- Recent match results should have explanations from Gemini
SELECT 
  pr.customer_name,
  pmr.created_at,
  COUNT(mcr.id) as match_count,
  AVG(mcr.match_score) as avg_score
FROM project_match_result pmr
JOIN project_request pr ON pr.id = pmr.project_request_id
LEFT JOIN match_candidate_result mcr ON mcr.match_result_id = pmr.id
WHERE pmr.created_at > NOW() - INTERVAL '1 hour'
GROUP BY pr.customer_name, pmr.created_at
ORDER BY pmr.created_at DESC;
```

**Verify CV file URIs are cached:**
```sql
-- Check if CVs have Gemini file URIs
SELECT 
  c.name,
  cv.gemini_file_uri,
  cv.updated_at
FROM consultant_cv cv
JOIN consultant c ON c.id = cv.consultant_id
WHERE cv.gemini_file_uri IS NOT NULL
ORDER BY cv.updated_at DESC
LIMIT 10;
```

## Verification Checklist

After testing, verify the following:

### ✅ Batch Matching is Active
- [ ] Logs show "Using Gemini Files API batch matching"
- [ ] Logs show "[FILES API] Starting match..."
- [ ] Logs show "[STEP 4] Calling Gemini API with N file references in SINGLE request"
- [ ] No sequential "matchCandidate" calls in logs

### ✅ CV Upload and Caching Works
- [ ] First run: CVs uploaded to Gemini ("Successfully uploaded CV for X")
- [ ] Subsequent runs: Cached URIs used ("Using cached file URI for X")
- [ ] Database has `gemini_file_uri` values in `consultant_cv` table

### ✅ Fallback Logic Works
- [ ] On 503 error: Logs show "Model X overloaded (503), falling back to gemini-2.5-flash"
- [ ] Retry happens after 1 second delay
- [ ] Final results are still saved even with fallback

### ✅ Results are Correct
- [ ] Matches are persisted in `project_match_result` and `match_candidate_result` tables
- [ ] Scores are between 0.0 and 1.0 (decimal) in database
- [ ] Match explanations contain JSON-formatted reasons
- [ ] API endpoints return results correctly

### ✅ Performance Improvement
- [ ] Matching completes in < 30 seconds (vs > 5 minutes before)
- [ ] Only ONE Gemini generateContent call per project request
- [ ] CV uploads happen in parallel or are cached

## Troubleshooting

### Issue: "No consultants found in candidate pool"
**Cause:** Database has no consultant data  
**Fix:** Run consultant sync first
```bash
curl -X POST http://localhost:8080/api/consultants/sync/run
```

### Issue: "Failed to upload CV for X"
**Cause:** Invalid GEMINI_API_KEY or network issue  
**Fix:** 
1. Verify API key: `echo $GEMINI_API_KEY`
2. Check Gemini API status
3. Review error logs for details

### Issue: "Model gemini-2.5-pro not found"
**Cause:** Model name mismatch or region restriction  
**Fix:** Update `application-local.yaml`:
```yaml
gemini:
  matchingModel: gemini-2.0-flash-exp  # Try alternative model
```

### Issue: "Health check shows DOWN"
**Cause:** Flowcase or Gemini API keys not set (expected in local dev)  
**Impact:** None - matching still works  
**Fix:** Not required for testing, but you can set keys if desired

### Issue: "Port 8080 already in use"
**Fix:**
```bash
lsof -ti:8080 | xargs kill -9
```

## Performance Benchmarks

Expected performance improvements:

| Metric | Before (Sequential) | After (Batch) | Improvement |
|--------|---------------------|---------------|-------------|
| API Calls | 10-50 | 1 | 10-50x reduction |
| Matching Time | 5-15 minutes | 10-30 seconds | 10-30x faster |
| Cost per Match | $1-5 | $0.10-0.50 | 10x cheaper |
| Timeout Risk | High | Low | Much safer |

## Next Steps After Verification

Once you've verified batch matching works:

1. **Monitor production logs** for the first few runs
2. **Track metrics:**
   - Matching duration
   - Cache hit rate for CV uploads
   - Fallback frequency (gemini-2.5-pro → gemini-2.5-flash)
3. **Optimize if needed:**
   - Adjust `topN` (currently 10 candidates)
   - Tune candidate scoring weights (skills vs CV quality)
4. **Clean up legacy code** (if desired):
   - Mark old `CandidateMatchingService` as deprecated
   - Remove unused `GeminiMatchingStrategy` if not needed

## Questions or Issues?

Check the implementation plan:
- `IMPLEMENTATION_PLAN_BATCH_MATCHING_FALLBACK_2025-11-23.md`

Review the WARP.md guide:
- `/Users/tandersen/git/cloudberries-candidate-match/WARP.md`

---

**Happy Testing! 🚀**
