# Manual Integration Tests

## ProjectMatchingBatchIntegrationTest

This is a **manual integration test** for the Gemini Files API batch matching feature.

### Prerequisites

1. **Local Docker database running**:
   ```bash
   cd candidate-match
   docker-compose -f docker-compose-local.yaml up -d
   ```

2. **Database has consultant data** (synced from Flowcase):
   ```bash
   # Check consultant count
   docker exec cloudberries-postgres-local psql -U candidatematch -d candidatematch -c "SELECT COUNT(*) FROM consultant;"
   ```
   Should show > 0 consultants.

3. **Environment variables set**:
   - `GEMINI_API_KEY`: Your Gemini API key
   - `FLOWCASE_API_KEY`: Your Flowcase API key (for sync)
   - `FLOWCASE_BASE_URL`: https://cloudberries.flowcase.com/api

### How to Run

**From IntelliJ IDEA** (ONLY way that works):
1. **Temporarily comment out** the `@Disabled` annotation (line 57)
2. Make sure Docker database is running: `docker-compose -f docker-compose-local.yaml up -d`
3. Right-click on the test method
4. Select "Run 'testBatchMatchingWithFilesApi - manual verification'"
5. Watch the console output
6. **Re-enable @Disabled** after testing

**Why not from command line?**
Maven test runs trigger Zonky embedded database auto-configuration which overrides our datasource settings. IntelliJ's test runner doesn't have this problem.

### What the Test Does

1. Creates a realistic project request with skills: Kotlin, Spring Boot, PostgreSQL, React, TypeScript
2. Calls `GeminiFilesMatchingService.matchConsultantsWithFilesApi()`
3. Verifies:
   - CVs are uploaded to Gemini Files API (with caching)
   - Single batch ranking call is made (not sequential calls)
   - Results are returned with scores and justifications
   - Top 5 candidates are ranked

### Expected Output

Look for these log messages:
```
[FILES API] Starting match...
[STEP 1] Fetching candidate pool...
[STEP 2] Scoring consultants...
[STEP 3] Uploading CVs to Gemini...
[STEP 4] Calling Gemini API with N file references in SINGLE request
[RESULT] Returning N matched consultants
```

### Troubleshooting

**Error: "No consultants found in candidate pool"**
- Database is empty. Sync consultants from Flowcase first
- Or database is not running on port 5433

**Error: "Gemini API key not valid"**
- Set `GEMINI_API_KEY` environment variable
- Check API key is valid and has quota

**Error: "No CVs could be uploaded to Gemini"**
- Consultants exist but have no active CVs
- Check: `SELECT COUNT(*) FROM consultant_cv WHERE active = true;`
