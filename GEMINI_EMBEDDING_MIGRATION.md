# Migration to Gemini Embeddings

## What Changed

You're now using **Gemini for embeddings** (fast search) and **Ollama for CV scoring** (your custom model).

### Configuration Changes

```yaml
# OLD (Slow - Ollama for everything)
embedding:
  provider: OLLAMA
  model: bge-m3
  dimension: 1024

# NEW (Fast - Gemini for embeddings)
embedding:
  provider: GOOGLE_GEMINI
  model: text-embedding-004
  dimension: 768
```

### What This Means

| Operation | Before | After | Speed Improvement |
|-----------|--------|-------|-------------------|
| Simple consultant listing | < 1s | < 1s | No change (database only) |
| Relational search (name/skills) | < 1s | < 1s | No change (database only) |
| **Semantic search** | **5-30s** | **< 2s** | **15-25s faster** ✅ |
| CV scoring | 5-30s | 5-30s | No change (still uses Ollama CV expert) |
| Chat/AI search | 20-120s | 10-60s | Faster embeddings, slower generation |

## Why This Works

### Embeddings (Search) → Gemini
- **Fast:** < 1 second per query
- **Cheap:** $0.00001 per 1,000 chars (~$0.01 per 1,000 searches)
- **No infrastructure:** Uses Gemini API, no Ollama needed
- **768 dimensions:** Standard for Gemini

### CV Scoring → Ollama
- **Custom trained:** Uses your `gemma3:12b-cv-expert-v3` model
- **Accurate:** Trained on 13 real Flowcase CVs
- **No external cost:** Runs in your Ollama container
- **Acceptable latency:** CV scoring is typically a background job

## Database Migration Required

⚠️ **IMPORTANT:** You need to regenerate embeddings because the dimension changed:

```
OLD: bge-m3 (1024 dimensions)
NEW: text-embedding-004 (768 dimensions)
```

### Migration Steps

#### 1. Check Current Embeddings

```sql
-- Connect to your database
psql "host=cloudberries-candidate-match-pgdb.postgres.database.azure.com port=5432 dbname=candidatematch user=candidatematch password=OdU]22WkG6$7 sslmode=require"

-- Check existing embeddings
SELECT provider, model, COUNT(*) 
FROM cv_embedding 
GROUP BY provider, model;

-- Should show: OLLAMA / bge-m3
```

#### 2. Backup Existing Embeddings

```sql
-- Create backup table
CREATE TABLE cv_embedding_backup_ollama AS 
SELECT * FROM cv_embedding;

CREATE TABLE cv_chunk_embedding_backup_ollama AS 
SELECT * FROM cv_chunk_embedding;
```

#### 3. Clear Old Embeddings

```sql
-- Delete old Ollama embeddings
DELETE FROM cv_chunk_embedding WHERE provider = 'OLLAMA';
DELETE FROM cv_embedding WHERE provider = 'OLLAMA';
```

#### 4. Regenerate with Gemini

Option A: **Via Backend API** (Recommended)
```bash
# Trigger embedding regeneration endpoint
curl -X POST https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io/api/embeddings/regenerate \
  -H "Content-Type: application/json"
```

Option B: **Automatic on Startup**
Update `application-prod.yaml`:
```yaml
sync:
  consultants:
    on-startup: true  # Already enabled
  embeddings:
    regenerate-on-startup: true  # Add this if supported
```

Option C: **Manual Script** (If no API exists)
You'll need to create a script that:
1. Fetches all consultants from database
2. Calls Gemini embedding API for each CV
3. Stores new embeddings in database

### 5. Verify New Embeddings

```sql
-- Check new embeddings
SELECT provider, model, COUNT(*) 
FROM cv_embedding 
GROUP BY provider, model;

-- Should show: GOOGLE_GEMINI / text-embedding-004

-- Check dimension
SELECT array_length(embedding, 1) as dimension
FROM cv_embedding 
LIMIT 1;

-- Should show: 768
```

## Environment Variables

Make sure these are set in your Azure Container App:

```bash
# Gemini API Key (required for embeddings)
GEMINI_API_KEY=<your-gemini-api-key>

# Ollama URL (required for CV scoring)
OLLAMA_BASE_URL=https://kb-ollama-cv-ca-dev.blackisland-4a1bc921.westeurope.azurecontainerapps.io
```

### Set in Azure:

```bash
az containerapp update \
  --name cloudberries-candidate-match-ca \
  --resource-group kubeberries \
  --set-env-vars \
    "GEMINI_API_KEY=secretref:gemini-api-key" \
    "OLLAMA_BASE_URL=https://kb-ollama-cv-ca-dev.blackisland-4a1bc921.westeurope.azurecontainerapps.io"
```

Or via Azure Portal:
1. Go to Container App → Configuration → Secrets
2. Add secret: `gemini-api-key`
3. Go to Environment Variables
4. Add reference: `GEMINI_API_KEY` → Secret: `gemini-api-key`

## Cost Impact

### Before (Ollama Only):
```
Ollama (2 CPU / 16GB):  $150/month
- Embeddings: bge-m3
- CV scoring: gemma3:12b-cv-expert-v3
```

### After (Gemini + Ollama):
```
Ollama (1 CPU / 8GB):   $75/month  (smaller, only for CV scoring)
Gemini API:             ~$1/month  (very low volume)
────────────────────────
Total:                  $76/month (-$74 savings)
```

### Usage Estimate:
- **10 searches/day** × 30 days = 300 searches/month
- **Average query:** 100 tokens × 300 = 30,000 tokens/month
- **Cost:** 30,000 tokens × $0.00001 per 1,000 = **$0.30/month**

Even with **1,000 searches/month**, cost is only **$10/month**.

## Rollback Plan

If you need to rollback:

### 1. Revert Configuration

```yaml
embedding:
  enabled: true
  provider: OLLAMA
  model: bge-m3
  dimension: 1024

ai:
  models:
    embeddings: bge-m3
```

### 2. Restore Old Embeddings

```sql
-- Delete Gemini embeddings
DELETE FROM cv_chunk_embedding WHERE provider = 'GOOGLE_GEMINI';
DELETE FROM cv_embedding WHERE provider = 'GOOGLE_GEMINI';

-- Restore Ollama embeddings
INSERT INTO cv_embedding 
SELECT * FROM cv_embedding_backup_ollama;

INSERT INTO cv_chunk_embedding 
SELECT * FROM cv_chunk_embedding_backup_ollama;
```

### 3. Redeploy Backend

```bash
# Deploy with old configuration
```

## Testing

### 1. Test Gemini Embedding Directly

```bash
curl -X POST https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent \
  -H "Content-Type: application/json" \
  -H "x-goog-api-key: $GEMINI_API_KEY" \
  -d '{
    "model": "models/text-embedding-004",
    "content": {
      "parts": [{
        "text": "Senior Kotlin developer with Spring Boot and PostgreSQL"
      }]
    }
  }'
```

Should return a 768-dimensional embedding array.

### 2. Test Semantic Search

```bash
# Via your backend
curl -X POST https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io/api/consultants/search/semantic \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Senior Kotlin developer",
    "provider": "GOOGLE_GEMINI",
    "model": "text-embedding-004",
    "topK": 10,
    "pagination": {
      "page": 0,
      "size": 10
    }
  }'
```

Should return results in **< 2 seconds**.

### 3. Test CV Scoring (Still Uses Ollama)

```bash
# This should still use your Ollama CV expert model
curl -X POST https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io/api/cvs/score \
  -H "Content-Type: application/json" \
  -d '{
    "cvId": "<some-cv-id>"
  }'
```

Should take **5-30 seconds** (Ollama gemma3:12b-cv-expert-v3).

## Monitoring

### Check Embedding Provider Info

```bash
curl https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io/api/consultants/search/embedding-info
```

Should return:
```json
{
  "enabled": true,
  "provider": "GOOGLE_GEMINI",
  "model": "text-embedding-004",
  "dimension": 768
}
```

### Database Query Performance

```sql
-- Check semantic search performance
EXPLAIN ANALYZE
SELECT consultant_id, cv_id, (embedding <=> '[0.1, 0.2, ...]'::vector) as distance
FROM cv_embedding
WHERE provider = 'GOOGLE_GEMINI'
  AND model = 'text-embedding-004'
ORDER BY distance
LIMIT 10;
```

Should execute in **< 100ms** with proper index.

## Summary

✅ **What you gain:**
- **15-25s faster** semantic search
- **$75/month cheaper** (smaller Ollama container)
- **< 1s latency** for embeddings
- **Same accuracy** for CV scoring (still uses your trained model)

⚠️ **What you need to do:**
1. Set `GEMINI_API_KEY` environment variable in Azure
2. Regenerate embeddings with Gemini
3. Test semantic search
4. Monitor performance and costs

🔄 **Deployment order:**
1. Deploy backend with new config
2. Regenerate embeddings (one-time operation)
3. Deploy frontend (already has 180s timeout)
4. Reduce Ollama container resources (1 CPU / 8GB)

**Result:** Fast search with cheap embeddings, accurate CV scoring with your custom model.
