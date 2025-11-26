# Gemini Model Strategy - Two Models for Two Jobs

## 🎯 Strategy Overview

**Problem:** Using one model for everything causes rate limiting and quality issues.

**Solution:** Use different models for different tasks:
1. **Gemini 3 Pro Preview** - For deep PDF analysis (runs once per upload)
2. **Gemini 2.5 Pro** - For batch candidate ranking (runs many times)

## Configuration

### application-local.yaml

```yaml
gemini:
  apiKey: ${GEMINI_API_KEY}
  
  # Model for project analysis (PDF upload, Må/Bør-krav extraction)
  # gemini-3-pro-preview gives best quality for requirement extraction
  # If you get 503 (overloaded), switch to gemini-2.5-pro
  model: gemini-3-pro-preview
  
  # Model for candidate ranking (batch operations)
  # gemini-2.5-pro is the workhorse: stable, 2M context window, high rate limits
  matchingModel: gemini-2.5-pro
  
  # Fast model for quick operations
  flashModel: gemini-2.5-flash
  
  fileStoreName: candidate-cv-store
  useFilesApi: true
```

## Model Selection Rationale

### 1. PDF Analysis: `gemini-3-pro-preview`

**Why?**
- ✅ **Best quality** for extracting structured requirements (Må/Bør-krav)
- ✅ **Runs infrequently** (only when user uploads PDF)
- ✅ **Complex reasoning** needed to understand context

**When to use:**
- PDF upload and analysis
- Requirement extraction
- Complex project descriptions

**Trade-offs:**
- ⚠️ Slower response (~8-10s)
- ⚠️ Lower rate limits (preview model)
- ⚠️ Can give 503 errors when overloaded

**Fallback:** If you get 503 errors consistently, switch to `gemini-2.5-pro`

### 2. Batch Ranking: `gemini-2.5-pro`

**Why?**
- ✅ **Stable production model** (not preview)
- ✅ **High rate limits** (handles many requests)
- ✅ **2M context window** (can analyze many CVs at once)
- ✅ **Good quality** (better than Flash, almost as good as 3.0)

**When to use:**
- Candidate ranking with Files API
- Batch processing of 10-50 consultants
- Any operation that runs multiple times per request

**Trade-offs:**
- ⚖️ Slightly lower quality than 3.0 Pro Preview
- ⚖️ Medium speed (~5-7s per request)

### 3. Quick Operations: `gemini-2.5-flash`

**Why?**
- ✅ **Very fast** (~2-3s)
- ✅ **Very high rate limits**
- ✅ **Low cost**

**When to use:**
- Quick queries
- Testing
- Non-critical operations

## How It Works

### PDF Upload Flow

```
1. User uploads PDF
   ↓
2. gemini-3-pro-preview analyzes PDF
   ↓ (extracts Må/Bør-krav, skills, etc.)
3. System stores requirements in database
   ↓
4. gemini-2.5-pro ranks 10-50 consultants (batch)
   ↓
5. Results displayed to user
```

### Implementation

**GeminiProperties** now has two model fields:

```kotlin
data class GeminiProperties(
    val apiKey: String,
    val model: String,           // For PDF analysis (gemini-3-pro-preview)
    val matchingModel: String,   // For batch ranking (gemini-2.5-pro)
    val flashModel: String,      // For quick queries
    val fileStoreName: String,
    val useFilesApi: Boolean = true
)
```

**Usage in code:**

```kotlin
// PDF analysis (ProjectRequestAnalysisService)
val model = geminiProperties.model  // gemini-3-pro-preview

// Batch ranking (GeminiFilesApiAdapter)
val model = geminiProperties.matchingModel  // gemini-2.5-pro
```

## Performance Comparison

### Before (Single Model)

| Task | Model | Time | Issue |
|------|-------|------|-------|
| PDF analysis | gemini-2.5-pro | ~5s | ✅ OK |
| Rank 30 consultants (sequential) | gemini-2.5-pro | 5min | ❌ Too slow |
| Rank 30 consultants (batch) | gemini-3-pro-preview | 60s | ❌ 503 errors |

### After (Two Models)

| Task | Model | Time | Result |
|------|-------|------|--------|
| PDF analysis | gemini-3-pro-preview | ~8s | ✅ Best quality |
| Rank 30 consultants (batch) | gemini-2.5-pro | 30-60s | ✅ Stable |

## Troubleshooting

### Problem: 503 Service Unavailable on PDF analysis

**Cause:** `gemini-3-pro-preview` is overloaded

**Solution:** Change `gemini.model` to `gemini-2.5-pro`

```yaml
gemini:
  model: gemini-2.5-pro  # Fallback from gemini-3-pro-preview
```

### Problem: 404 Not Found

**Cause:** Model name doesn't exist in your region/account

**Solution:** Check available models:

```bash
curl -s "https://generativelanguage.googleapis.com/v1beta/models?key=$GEMINI_API_KEY" | \
  jq -r '.models[] | select(.supportedGenerationMethods | contains(["generateContent"])) | .name'
```

### Problem: Still getting 503 on batch ranking

**Cause:** Too many requests too fast

**Solution:** Already using `gemini-2.5-pro` which has high limits. If still failing:
1. Reduce batch size (process 10 instead of 30 consultants)
2. Add retry logic with exponential backoff
3. Switch to `gemini-2.5-flash` (faster, lower quality)

## Environment Variables (Production)

```bash
# Override defaults with environment variables
export GEMINI_MODEL="gemini-3-pro-preview"          # PDF analysis
export GEMINI_MATCHING_MODEL="gemini-2.5-pro"      # Batch ranking
export GEMINI_FLASH_MODEL="gemini-2.5-flash"       # Quick queries
export GEMINI_API_KEY="your-api-key"
```

## Cost Estimation

| Model | Cost per 1K tokens | Typical usage |
|-------|-------------------|---------------|
| gemini-3-pro-preview | ~$0.02 | 1 request per PDF upload |
| gemini-2.5-pro | ~$0.01 | 1 request per matching (10-50 CVs) |
| gemini-2.5-flash | ~$0.0005 | Multiple quick queries |

**Example:** 
- Upload 1 PDF → Analyze with 3.0 Pro → Match 30 consultants with 2.5 Pro
- Cost: $0.02 (analysis) + $0.30 (matching ~30K tokens) = **~$0.32 per project**

## Testing Different Models

Want to test a different model? Just change the config:

```yaml
# Test with 2.5 Pro for everything
gemini:
  model: gemini-2.5-pro
  matchingModel: gemini-2.5-pro

# Test with Flash for speed
gemini:
  model: gemini-2.5-pro
  matchingModel: gemini-2.5-flash

# Use 3.0 Pro for everything (if rate limits allow)
gemini:
  model: gemini-3-pro-preview
  matchingModel: gemini-3-pro-preview  # ⚠️ May get 503 errors
```

## Summary

✅ **Implemented:**
- Separate models for PDF analysis vs batch ranking
- gemini-3-pro-preview for best quality PDF analysis
- gemini-2.5-pro for stable batch operations
- Easy switching via config files

✅ **Benefits:**
- Better quality for PDF analysis
- Stable batch ranking (no 503 errors)
- Optimized for performance and rate limits

🎯 **Recommendation:**
- **Keep current config** (3-pro-preview + 2.5-pro)
- **Monitor for 503 errors** on PDF analysis
- **Fallback to 2.5-pro** if issues occur
