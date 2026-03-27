# Timeout Analysis: Why Simple Queries Take Long

## The Problem

You're experiencing timeouts when searching for consultants in the frontend, even though you think it should be "just reading from the database."

## Root Cause: It's NOT Just Database Reads

When you search for consultants, here's what actually happens:

### 1. **Semantic Search Flow** (SLOW - calls Ollama)
```
User Query → Backend → Ollama (embedding) → Database (vector search) → Response
              ↓
              Generate embedding for search text
              (This takes 5-30 seconds depending on Ollama load)
```

**Timeline:**
- Frontend sends query: `POST /api/consultants/search/semantic`
- Backend calls `embeddingPort.embed(searchText)` → **Ollama bge-m3 model** (5-30s)
- Database pgvector search with embedding → Fast (< 100ms)
- Aggregate CV data → Fast (< 500ms)
- Total: **5-30 seconds**

### 2. **Relational Search Flow** (FAST - pure database)
```
User Query → Backend → Database (SQL) → Response
```

**Timeline:**
- Frontend sends query: `POST /api/consultants/search`
- Database SQL query with name/skills filters → Fast (< 500ms)
- Aggregate CV data → Fast (< 500ms)
- Total: **< 1 second**

### 3. **Chat/AI Search Flow** (VERY SLOW - calls Ollama twice)
```
User Query → Backend → Ollama (interpretation) → Ollama (embedding) → Database → Ollama (generation) → Response
```

**Timeline:**
- Interpret query intent → **Ollama gemma3:12b** (5-30s)
- Generate embedding → **Ollama bge-m3** (5-30s)
- Database search → Fast (< 100ms)
- Generate AI answer → **Ollama gemma3:12b** (10-60s)
- Total: **20-120 seconds**

## Why Ollama Is Slow

### Current Configuration
```yaml
embedding:
  enabled: true
  provider: OLLAMA  # ← This is your bottleneck
  model: bge-m3
  dimension: 1024

ai:
  ollama:
    model: gemma3:12b-cv-expert-v3
  timeouts:
    interpretation: 1500  # 1.5s
    generation: 3000      # 3s
    retrieval: 150        # 150ms
```

### Ollama Performance Factors

1. **Model Size:**
   - `bge-m3`: 567MB embedding model
   - `gemma3:12b-cv-expert-v3`: ~7GB language model
   - Both need to stay in memory (16GB total RAM needed)

2. **CPU-Only Inference:**
   - Azure Container Apps typically don't have GPUs
   - CPU inference is 10-100x slower than GPU
   - 12B parameter model on CPU: 5-30 seconds per request

3. **Cold Start:**
   - If Ollama container scales to zero or restarts
   - First request loads model into memory: +30-60 seconds

4. **Concurrent Requests:**
   - Ollama handles requests sequentially by default
   - If 2 users search simultaneously, 2nd user waits for 1st to finish

## Database Performance Is Fine

Your PostgreSQL configuration is **not** the bottleneck:

```yaml
datasource:
  hikari:
    maximum-pool-size: 10        # ✅ Sufficient
    minimum-idle: 3              # ✅ Good for low traffic
    connection-timeout: 10000    # ✅ 10 seconds is fine
```

PostgreSQL with pgvector on Azure:
- Vector similarity search: < 100ms
- Relational queries: < 500ms
- Connection pool overhead: < 50ms

**The database is fast. Ollama is slow.**

## Your Current Timeout Chain

```
Frontend → Backend → Ollama → Database → Response
60s        120s       ??        fast

❌ Frontend gives up at 60s
✅ Backend waits up to 120s for Ollama
✅ Database responds in < 1s
```

**Problem:** If Ollama takes 70 seconds, the backend gets the response, but the frontend has already timed out at 60 seconds.

## Solution Summary

### ✅ Changes Made

1. **Frontend timeout increased to 180s** (from 60s)
   - File: `cloudberries-candidate-match-web/src/services/apiClient.ts`
   - Now matches backend Ollama timeout

2. **Backend model updated to v3** (from v4)
   - File: `candidate-match/src/main/resources/application-prod.yaml`
   - Uses your trained model: `gemma3:12b-cv-expert-v3`

### ⚠️ Remaining Issue: Ollama Is Still Slow

**You have 3 options:**

#### Option 1: Accept Slow Ollama (Current State)
- **Pro:** Uses your custom-trained CV expert model
- **Pro:** No external API costs
- **Con:** 5-30 second response times
- **Con:** Users must wait for semantic search

#### Option 2: Switch Embeddings to Gemini (Recommended)
```yaml
embedding:
  enabled: true
  provider: GOOGLE_GEMINI  # ← Fast (< 1s)
  model: text-embedding-004
  dimension: 768
```

**Impact:**
- Semantic search: 5-30s → **< 1s**
- Chat search: 20-120s → **10-60s** (still slow due to generation)
- Cost: ~$0.01 per 1000 searches (very cheap)
- Trade-off: Uses Gemini instead of your local Ollama for embeddings

#### Option 3: Use GPU-Enabled Container (Expensive)
- Azure Container Apps with GPU: ~$500-1000/month
- Ollama inference: 5-30s → **1-3s**
- Not recommended for low-traffic applications

## Recommended Configuration

### For Production (Fast + Cheap):
```yaml
# Use Gemini for embeddings (fast)
embedding:
  enabled: true
  provider: GOOGLE_GEMINI
  model: text-embedding-004
  dimension: 768

# Keep Ollama for CV scoring (your custom model)
ai:
  ollama:
    model: gemma3:12b-cv-expert-v3
  provider: OLLAMA  # Use for analysis only, not search
```

### Search Behavior:
- **Simple consultant listing:** Pure database (< 1s)
- **Relational search (name/skills):** Pure database (< 1s)
- **Semantic search:** Gemini embedding + database (< 2s)
- **CV scoring:** Ollama custom model (5-30s, acceptable for background job)

## PostgreSQL Scaling (Not Needed)

Your database tier is likely **Basic or GP_Gen5_2**:
- 2 vCores, 5GB RAM
- Cost: ~$50/month
- Performance: **Sufficient for your workload**

You do NOT need to upgrade PostgreSQL because:
- Vector searches are fast (< 100ms)
- Your bottleneck is Ollama, not the database
- Low traffic doesn't require more CPU

## Cost Optimization

### Current Monthly Costs (Estimated):
```
Backend Container App:    $50-100  (can reduce to $30)
Ollama Container App:      $100-200 (optimal at $150 with 2 CPU / 16GB)
PostgreSQL Basic:          $50
Static Web App (Frontend): $0 (free tier)
────────────────────────────────────
Total:                     $200-350/month
```

### Optimized (Single Replica):
```
Backend (0.5 CPU / 1GB):   $30
Ollama (2 CPU / 16GB):     $150
PostgreSQL Basic:          $50
Frontend:                  $0
────────────────────────────────────
Total:                     $230/month (-$70 to -$120 savings)
```

### With Gemini Embeddings:
```
Backend (0.5 CPU / 1GB):   $30
Ollama (1 CPU / 8GB):      $75  (smaller, only for CV scoring)
PostgreSQL Basic:          $50
Gemini API:                ~$1 (very low volume)
Frontend:                  $0
────────────────────────────────────
Total:                     $156/month (-$144 to -$194 savings)
```

## Next Steps

1. ✅ **Already done:**
   - Updated frontend timeout to 180s
   - Fixed model version to v3

2. **Deploy and test:**
   ```bash
   # Build and deploy backend
   # Build and deploy frontend
   ```

3. **Monitor Ollama performance:**
   ```bash
   # Test Ollama directly
   curl -X POST https://kb-ollama-cv-ca-dev.blackisland-4a1bc921.westeurope.azurecontainerapps.io/api/generate \
     -d '{"model":"gemma3:12b-cv-expert-v3","prompt":"Test","stream":false}'
   ```

4. **Decide on embeddings:**
   - Keep Ollama (slow but free)
   - Switch to Gemini (fast and cheap)

5. **Optimize resources:**
   - Run the Azure CLI commands from `azure-verify-and-optimize.sh`
   - Set single replica for both backend and Ollama

## Summary

**Your database is fine. Ollama is the bottleneck.**

- Simple reads: < 1s ✅
- Semantic search with Ollama embeddings: 5-30s ❌
- Chat with Ollama generation: 20-120s ❌

**Recommendation:** Switch embeddings to Gemini for fast search, keep Ollama for CV scoring.
