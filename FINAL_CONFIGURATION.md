# Final Configuration: Local bge-m3 + Ollama

## ✅ What You Have Now

### Single Ollama Container with Two Models

**One container serves both:**
- **bge-m3:** Fast embeddings for semantic search (1-3s)
- **gemma3:12b-cv-expert-v3:** Your trained model for CV scoring (5-30s)

### Configuration

```yaml
# Embeddings: bge-m3 (local, fast, free)
embedding:
  enabled: true
  provider: OLLAMA
  model: bge-m3
  dimension: 1024

# CV Scoring: Your custom trained model
ai:
  ollama:
    model: gemma3:12b-cv-expert-v3
  models:
    interpretation: gemma3:12b-cv-expert-v3
    generation_default: gemma3:12b-cv-expert-v3
    generation_quality: gemma3:12b-cv-expert-v3
    embeddings: bge-m3
```

### Frontend Timeout

```typescript
// Frontend: 3 minutes (enough for Ollama)
timeout: 180000  // 180 seconds
```

## 📊 Performance & Cost

### Performance
| Operation | Time | Model Used |
|-----------|------|------------|
| Simple listing | < 1s | Database only |
| Relational search | < 1s | Database only |
| Semantic search | 1-3s | bge-m3 (Ollama) |
| CV scoring | 5-30s | gemma3:12b-cv-expert-v3 (Ollama) |

### Cost (Single Ollama Container)
```
Ollama (2 CPU / 12GB):  $120/month
  - bge-m3 (embeddings)
  - gemma3:12b-cv-expert-v3 (CV scoring)

Backend (0.5 CPU / 1GB):  $30/month
PostgreSQL:               $50/month
Frontend:                 $0 (static)
─────────────────────────────────
Total:                    $200/month
```

**vs. Gemini option:** $156/month + external API dependency

## 🚀 Deployment Steps

### 1. Ensure Ollama Has Both Models

SSH into your Ollama container or use the API:

```bash
# Check what models are loaded
curl https://kb-ollama-cv-ca-dev.blackisland-4a1bc921.westeurope.azurecontainerapps.io/api/tags

# Should show:
# - bge-m3 (or bge-m3:latest)
# - gemma3:12b-cv-expert-v3
```

If `bge-m3` is missing, pull it:

```bash
# Via Ollama CLI (if you have shell access)
ollama pull bge-m3

# Or via API (if Ollama supports it)
curl -X POST https://kb-ollama-cv-ca-dev.../api/pull \
  -d '{"name": "bge-m3"}'
```

### 2. Set Ollama Container Resources

```bash
# Ollama needs enough RAM for both models
az containerapp update \
  --name kb-ollama-cv-ca-dev \
  --resource-group kubeberries \
  --cpu 2.0 --memory 12Gi \
  --min-replicas 1 --max-replicas 1

# Memory breakdown:
# - bge-m3: ~600MB
# - gemma3:12b-cv-expert-v3: ~7GB
# - System overhead: ~1GB
# - Buffer: ~3GB
# Total: 12GB (safe)
```

### 3. Deploy Backend

```bash
cd /Users/tandersen/git/cloudberries-candidate-match

# Commit changes
git add candidate-match/src/main/resources/application-prod.yaml
git add candidate-match/src/main/kotlin/no/cloudberries/candidatematch/controllers/embedding/EmbeddingController.kt
git commit -m "Use bge-m3 for embeddings, add admin-only comments to embedding endpoints"
git push

# Deploy backend (your deployment process)
```

### 4. Deploy Frontend

```bash
cd /Users/tandersen/git/cloudberries-candidate-match-web

# Already done - timeout increased to 180s
git add src/services/apiClient.ts
git commit -m "Increase timeout to 180s (3 minutes) for Ollama operations"
git push

# Deploy frontend (your deployment process)
```

### 5. Optimize Backend Resources

```bash
# Backend: Single replica, minimal resources
az containerapp update \
  --name cloudberries-candidate-match-ca \
  --resource-group kubeberries \
  --cpu 0.5 --memory 1.0Gi \
  --min-replicas 1 --max-replicas 1
```

## 🔒 Security: Admin-Only Endpoints

### Current State
The embedding endpoints are **NOT** secured yet:
- `/api/embeddings/run/jason`
- `/api/embeddings/run` (userId, cvId)
- `/api/embeddings/run/missing` (batch processing)

### Documentation Added
Added clear comments in `EmbeddingController.kt`:
```kotlin
/**
 * ADMIN ONLY: Embedding management endpoints
 * 
 * TODO: Add Spring Security with @PreAuthorize("hasRole('ADMIN')")
 * These endpoints should only be accessible to administrators because:
 * - They can trigger expensive operations (Ollama embeddings for all CVs)
 * - They modify system-wide data (embeddings table)
 * - They can cause performance impact (batch processing)
 */
```

### Temporary Mitigation
Until Spring Security is implemented:

1. **Document internally** that these are admin-only
2. **Don't advertise** these endpoints publicly
3. **Monitor usage** via logs
4. **Consider firewall rules** to restrict access to specific IPs

### Future: Spring Security Implementation
When you add Spring Security:

```kotlin
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/run/missing")
fun runMissing(...): ResponseEntity<Map<String, Any>> {
    // ...
}
```

## 🧪 Testing

### 1. Test Ollama Embeddings (bge-m3)

```bash
# Test embedding generation
curl -X POST https://kb-ollama-cv-ca-dev.blackisland-4a1bc921.westeurope.azurecontainerapps.io/api/embed \
  -H "Content-Type: application/json" \
  -d '{
    "model": "bge-m3",
    "input": "Senior Kotlin developer with Spring Boot"
  }'

# Should return embedding array with 1024 dimensions
```

### 2. Test Ollama CV Scoring (gemma3:12b-cv-expert-v3)

```bash
# Test generation
curl -X POST https://kb-ollama-cv-ca-dev.blackisland-4a1bc921.westeurope.azurecontainerapps.io/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemma3:12b-cv-expert-v3",
    "prompt": "Evaluate this CV: John Doe, 5 years Java",
    "stream": false
  }'

# Should return JSON response with CV evaluation
```

### 3. Test Backend Semantic Search

```bash
# Test via backend (uses bge-m3 embeddings)
curl -X POST https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io/api/consultants/search/semantic \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Senior Kotlin developer",
    "provider": "OLLAMA",
    "model": "bge-m3",
    "topK": 10,
    "pagination": {"page": 0, "size": 10}
  }'

# Should return results in 1-3 seconds
```

### 4. Test Embedding Info Endpoint

```bash
curl https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io/api/consultants/search/embedding-info

# Should return:
# {
#   "enabled": true,
#   "provider": "OLLAMA",
#   "model": "bge-m3",
#   "dimension": 1024
# }
```

## ✅ Benefits of This Setup

### vs. Gemini Embeddings:
✅ **No external API** - fully self-hosted
✅ **No dimension change** - already 1024D in database
✅ **No regeneration needed** - existing embeddings work
✅ **No API costs** - only infrastructure
✅ **Better control** - you own the data
✅ **Offline capable** - doesn't need internet

### vs. Two Separate Ollama Containers:
✅ **Simpler architecture** - one container to manage
✅ **Lower cost** - one container fee, not two
✅ **Easier maintenance** - single deployment
✅ **Shared resources** - efficient memory usage

## 📝 Files Modified

1. **Backend config:** `candidate-match/src/main/resources/application-prod.yaml`
   - Changed `embedding.provider` from `GOOGLE_GEMINI` back to `OLLAMA`
   - Changed `embedding.model` from `text-embedding-004` back to `bge-m3`
   - Changed `embedding.dimension` from `768` back to `1024`
   - Fixed `ai.ollama.model` from `v4` to `v3`

2. **Embedding controller:** `EmbeddingController.kt`
   - Added admin-only security documentation
   - Documented that these endpoints need Spring Security

3. **Frontend:** `apiClient.ts`
   - Increased timeout from 60s to 180s (3 minutes)

## 🎯 Architecture

```
┌─────────────────────────────────────────────────┐
│ Frontend (Static Web App)                       │
│ - Timeout: 180s (3 min)                         │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│ Backend (Container App)                          │
│ - 0.5 CPU / 1GB RAM                             │
│ - 1 replica (always on)                          │
│ - Cost: $30/month                                │
└──────┬──────────────────────┬───────────────────┘
       │                      │
       │ Embeddings           │ CV Scoring
       │ (1-3s)              │ (5-30s)
       ▼                      ▼
┌──────────────────────────────────────────────────┐
│ Ollama Container (Single Instance)               │
│ - 2 CPU / 12GB RAM                               │
│ - 1 replica (always on)                          │
│                                                  │
│ Models loaded:                                   │
│  ├─ bge-m3 (~600MB)                             │
│  └─ gemma3:12b-cv-expert-v3 (~7GB)              │
│                                                  │
│ - Cost: $120/month                               │
└──────────────────┬───────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────┐
│ PostgreSQL + pgvector                            │
│ - Stores 1024D embeddings (bge-m3)              │
│ - Fast vector search (< 100ms)                   │
│ - Cost: $50/month                                │
└─────────────────────────────────────────────────┘

Total: $200/month
```

## 🔧 Troubleshooting

### Semantic search times out
1. Check Ollama has `bge-m3` loaded: `curl .../api/tags`
2. Verify backend timeout config: `search.ollama.read-timeout-seconds: 120`
3. Check Ollama container has 12GB RAM

### CV scoring fails
1. Verify `gemma3:12b-cv-expert-v3` is loaded
2. Check Ollama logs for memory issues
3. Ensure container has 2 CPU + 12GB RAM

### Frontend shows "Request timeout"
1. Check frontend timeout is 180s: `apiClient.ts`
2. Verify backend is responding: Check logs
3. Test Ollama directly: `curl .../api/generate`

## 📚 Documentation Files

1. **This file** - Final configuration summary
2. `TIMEOUT_ANALYSIS.md` - Why timeouts happened
3. `azure-verify-and-optimize.sh` - Verification script
4. `CHANGES_SUMMARY.md` - Previous iteration (Gemini)
5. `GEMINI_EMBEDDING_MIGRATION.md` - Alternative approach (not used)

## 🎉 Summary

**Problem Solved:**
- Frontend timeout fixed (60s → 180s)
- Model version fixed (v4 → v3)
- Using local bge-m3 for embeddings (no Gemini needed)

**Result:**
- Fast semantic search (1-3s with bge-m3)
- Custom CV scoring (your trained model)
- Single Ollama container (both models)
- Lower cost than Gemini approach
- No external dependencies
- Admin endpoints documented for future security

**Next:** Deploy and test! 🚀
