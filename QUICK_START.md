# Quick Start - Gemini Files API Testing

## Prerequisites

```bash
# Ensure you have these tools
java -version  # Should be 21.x
mvn -version   # Should be 3.8+
docker -v      # For PostgreSQL

# Set API key
export GEMINI_API_KEY="your-actual-api-key-here"
```

## Start Database

```bash
cd /Users/tandersen/git/cloudberries-candidate-match/candidate-match
docker-compose -f docker-compose-local.yaml up -d

# Verify running
docker-compose -f docker-compose-local.yaml ps
```

## Build & Run Application

```bash
cd /Users/tandersen/git/cloudberries-candidate-match/candidate-match

# Clean build
mvn clean package -DskipTests

# Run with local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Verify Implementation

### 1. Check Database Migration

```bash
# Connect to PostgreSQL
psql "host=localhost port=5433 dbname=candidatematch user=candidatematch password=candidatematch123"

# Check for new column
\d consultant_cv

# Should show:
# gemini_file_uri | character varying(512) |

\q
```

### 2. Check Application Logs

Look for these startup messages:
```
INFO  o.s.s.a.EnableAsync - @Async enabled
INFO  AsyncConfig - Async configuration loaded
INFO  GeminiProperties - useFilesApi=true
```

### 3. Test Existing Endpoint

The Files API integrates transparently with existing endpoints:

```bash
# Get matches for an existing project request
curl http://localhost:8080/api/matches/requests/30/top-consultants?limit=5
```

**Expected Log Output**:
```
[FILES API] Starting match for project 30 (topN=5)
[STEP 1] Fetching candidate pool with 15 required skills
[STEP 1] Retrieved 42 consultants from database
[STEP 2] Scoring consultants by 50% skills + 50% CV quality
[STEP 2] Selected 10 consultants for Gemini evaluation
[STEP 3] Uploading CVs to Gemini Files API (with caching)
[STEP 4] Calling Gemini API with 10 file references in SINGLE request
[STEP 4] Gemini returned 5 ranked candidates
```

### 4. Verify Caching

```sql
-- In psql
SELECT consultant_id, LEFT(gemini_file_uri, 50) as uri_preview
FROM consultant_cv
WHERE gemini_file_uri IS NOT NULL
LIMIT 5;
```

Should show URIs like:
```
 consultant_id | uri_preview
---------------+----------------------------------------------------
           123 | https://generativelanguage.googleapis.com/v1beta/f
```

## Toggle Feature On/Off

### Disable Files API (use inline CVs)

```yaml
# Edit application-local.yaml
gemini:
  useFilesApi: false
```

Restart application → will use inline CV approach

### Enable Files API

```yaml
# Edit application-local.yaml
gemini:
  useFilesApi: true
```

Restart application → will use Files API approach

## Common Issues

### Issue: 400 Bad Request

```bash
# Check model name in logs
grep "gemini.model" logs/application.log

# Should be: gemini-1.5-pro or gemini-1.5-flash
# NOT: gemini-3-pro-preview (doesn't exist on v1beta)
```

### Issue: No Gemini API Key

```bash
# Check environment
echo $GEMINI_API_KEY

# If empty, set it:
export GEMINI_API_KEY="your-key-here"

# Restart application
```

### Issue: Database Connection Failed

```bash
# Check PostgreSQL is running
docker-compose -f candidate-match/docker-compose-local.yaml ps

# Should show:
# cloudberries-postgres-local   Up   5433->5432/tcp

# If not running:
docker-compose -f candidate-match/docker-compose-local.yaml up -d
```

## Performance Testing

### Test Cache Performance

```bash
# First request (no cache)
time curl http://localhost:8080/api/matches/requests/30/top-consultants

# Note the time: ~5-8 seconds (includes uploads)

# Second request (with cache)
time curl http://localhost:8080/api/matches/requests/30/top-consultants

# Note the time: ~2-3 seconds (skips uploads)
```

### Check Cache Hit Ratio

```sql
-- Total consultants
SELECT COUNT(*) FROM consultant_cv;

-- Consultants with cached URIs
SELECT COUNT(*) FROM consultant_cv WHERE gemini_file_uri IS NOT NULL;

-- Calculate ratio
SELECT 
  ROUND(100.0 * COUNT(*) FILTER (WHERE gemini_file_uri IS NOT NULL) / COUNT(*), 2) as cache_hit_percent
FROM consultant_cv;
```

## Next Steps

1. ✅ Verify database migration applied
2. ✅ Test existing match endpoint
3. ✅ Check logs for Files API messages
4. ✅ Verify caching works
5. ✅ Compare performance with/without cache

## Full Documentation

See `GEMINI_FILES_API_IMPLEMENTATION.md` for complete details:
- Architecture overview
- All files created
- Configuration options
- Troubleshooting guide
- Performance metrics

## Support

If issues persist:
1. Check application logs: `tail -f logs/spring.log`
2. Check database logs: `docker-compose -f candidate-match/docker-compose-local.yaml logs postgres-local`
3. Verify API key is valid: Test with curl to Gemini directly
4. Review implementation doc for detailed troubleshooting
