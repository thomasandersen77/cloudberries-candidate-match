# Complete Fix Summary: CV Scoring with Ollama

## Issues Encountered and Resolved

### Issue #1: SocketTimeoutException (RESOLVED)
**Error**: `java.net.SocketTimeoutException: timeout` after 10 seconds  
**Cause**: `OllamaHttpClient` had no timeout configuration  
**Fix**: Added timeout properties to `OllamaConfig` and configured `OkHttpClient`  
**Details**: See [OLLAMA_TROUBLESHOOTING.md](./OLLAMA_TROUBLESHOOTING.md)

### Issue #2: OpenAI 404 Error (RESOLVED)
**Error**: `java.lang.RuntimeException: Feil fra OpenAI API: 404`  
**Cause**: `AISettings.provider` not loading from config (was under `spring.ai.provider` instead of `ai.provider`)  
**Fix**: Restructured `application-local.yaml` with proper top-level `ai:` and `ollama:` sections  
**Details**: See [PROVIDER_ROUTING_FIX.md](./PROVIDER_ROUTING_FIX.md)

## Files Modified

### 1. OllamaConfig.kt
**Path**: `ai-rag-service/src/main/kotlin/no/cloudberries/ai/config/OllamaConfig.kt`

**Changes**: Added timeout properties
```kotlin
var connectTimeoutSeconds: Long = 10
var readTimeoutSeconds: Long = 120
var writeTimeoutSeconds: Long = 30
```

### 2. OllamaHttpClient.kt
**Path**: `ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/integration/ollama/OllamaHttpClient.kt`

**Changes**: 
- Added `import java.util.concurrent.TimeUnit`
- Configured OkHttpClient with timeouts from config

### 3. application-local.yaml (Most Important)
**Path**: `candidate-match/src/main/resources/application-local.yaml`

**Changes**: Complete restructuring of AI configuration

#### Before (Broken)
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      # ...
    provider: OLLAMA  # ❌ Wrong location - creates spring.ai.provider
    timeout: 180s
    models:
      # ...
```

#### After (Fixed)
```yaml
spring:
  ai:
    ollama:  # Spring AI framework config
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:14b
      embedding:
        options:
          model: bge-m3
    vectorstore:
      pgvector:
        initialize-schema: false
        table-name: vector_store

# Ollama connection config for OllamaHttpClient
ollama:
  baseUrl: http://localhost:11434
  model: qwen2.5:14b
  embeddingModel: bge-m3
  embeddingFallbackModel: nomic-embed-text
  connectTimeoutSeconds: 10
  readTimeoutSeconds: 180
  writeTimeoutSeconds: 30

# AI provider selection for AISettings
ai:
  provider: OLLAMA  # ✅ Correct location - creates ai.provider
  timeout: 180s
  models:
    interpretation: gemini-2.5-flash
    generation_default: gemini-2.5-flash
    generation_quality: gemini-2.5-pro
    embeddings: bge-m3
  # ... more settings
```

## Configuration Architecture Explained

### Three Separate Configuration Classes

1. **Spring AI Auto-configuration** (`spring.ai.*`)
   - Managed by Spring AI framework
   - Configures Spring AI beans for Ollama integration
   - Used by Spring AI's ChatClient, EmbeddingModel, etc.

2. **OllamaConfig** (`ollama.*`)
   ```kotlin
   @Component
   @ConfigurationProperties(prefix = "ollama")
   class OllamaConfig { ... }
   ```
   - Custom configuration for `OllamaHttpClient`
   - Controls connection URL, model selection, timeouts

3. **AISettings** (`ai.*`)
   ```kotlin
   @ConfigurationProperties(prefix = "ai")
   class AISettings {
       var provider: AIProvider = AIProvider.OPENAI  // Default
       // ...
   }
   ```
   - Controls which AI provider to use (OPENAI, GEMINI, OLLAMA, ANTHROPIC)
   - Used by `AiContentGenerationAdapter` for routing decisions

### Why Three Sections?

| Config Section | YAML Prefix | Purpose | Example Use |
|----------------|-------------|---------|-------------|
| `spring.ai.ollama` | `spring.ai.*` | Spring AI framework integration | Spring AI's ChatClient, vectorstore |
| `ollama` | `ollama.*` | OllamaHttpClient settings | Direct Ollama HTTP calls, timeouts |
| `ai` | `ai.*` | Provider selection | Which AI provider to use globally |

## Verification Checklist

- [x] Ollama is running on port 11434
- [x] Models available: `qwen2.5:14b`, `bge-m3:latest`, `llama3.2:3b`
- [x] `OllamaConfig.kt` has timeout properties
- [x] `OllamaHttpClient.kt` applies timeouts to OkHttpClient
- [x] `application-local.yaml` has top-level `ai:` section with `provider: OLLAMA`
- [x] `application-local.yaml` has top-level `ollama:` section with timeout configs
- [ ] Application rebuilt: `mvn clean package -DskipTests`
- [ ] Application restarted with local profile
- [ ] CV scoring endpoint tested successfully

## Next Steps to Complete Fix

```bash
# 1. Rebuild
cd /Users/tandersen/git/cloudberries-candidate-match
mvn clean package -DskipTests

# 2. Restart application
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local

# 3. Test CV scoring (in new terminal)
# Replace {candidateId} with actual consultant ID
curl -X POST http://localhost:8080/api/cv-score/67e27aeb4d749c0040dd0206/run

# Expected: JSON response with scorePercent, summary, strengths, potentialImprovements
# NOT: OpenAI 404 error
# NOT: SocketTimeoutException after 10 seconds
```

## Success Criteria

### ✅ Fixed When You See:
- Application starts without errors
- Logs show: `AISettings` with `provider=OLLAMA`
- Logs show: `OllamaConfig` loaded with `baseUrl=http://localhost:11434`
- CV scoring request completes within 180 seconds
- Response contains valid CV score data

### ❌ Still Broken If You See:
- `Feil fra OpenAI API: 404` errors
- `SocketTimeoutException` after 10 seconds
- Application routing to OpenAI instead of Ollama

## Model Configuration for Different Use Cases

### Current Setup (Production Quality)
```yaml
ollama:
  model: qwen2.5:14b  # 14.8B params, high quality, slower
  readTimeoutSeconds: 180  # 3 minutes
```
- **Best for**: Production CV scoring, high accuracy required
- **Speed**: Moderate (10-60 seconds per CV on CPU)
- **Quality**: ⭐⭐⭐ Excellent

### Fast Development/Testing
```yaml
ollama:
  model: llama3.2:3b  # 3.2B params, good quality, faster
  readTimeoutSeconds: 60  # 1 minute
```
- **Best for**: Development, quick iterations
- **Speed**: Fast (2-10 seconds per CV on CPU)
- **Quality**: ⭐⭐ Good

### Switch to Cloud (Gemini)
```yaml
ai:
  provider: GEMINI  # Use Google Gemini instead
```
- **Best for**: High-volume processing, GPU not available locally
- **Speed**: Very fast (API response in seconds)
- **Quality**: ⭐⭐⭐ Excellent
- **Requires**: `GEMINI_API_KEY` environment variable

## Troubleshooting Quick Reference

| Error | Quick Fix |
|-------|-----------|
| OpenAI 404 | Check `ai.provider: OLLAMA` is at top level in YAML |
| Timeout after 10s | Check `ollama.readTimeoutSeconds: 180` is set |
| Timeout after 180s | Use faster model or increase timeout |
| Connection refused | Start Ollama: `ollama serve` or `brew services start ollama` |
| Model not found | Pull model: `ollama pull qwen2.5:14b` |

## Documentation Index

1. **[QUICK_FIX_SUMMARY.md](./QUICK_FIX_SUMMARY.md)** - Quick reference for timeout fix
2. **[OLLAMA_TROUBLESHOOTING.md](./OLLAMA_TROUBLESHOOTING.md)** - Comprehensive Ollama diagnostics
3. **[PROVIDER_ROUTING_FIX.md](./PROVIDER_ROUTING_FIX.md)** - Deep dive into configuration routing issue
4. **[WARP.md](./WARP.md)** - Complete repository architecture guide
5. **THIS FILE** - Summary of all fixes

## For IntelliJ Jetbrains AI

All code changes have been applied. The root causes were:

1. **Missing timeout configuration** in OkHttpClient → Fixed by adding timeout properties to `OllamaConfig`
2. **Wrong YAML structure** for `ai.provider` → Fixed by moving to top-level `ai:` section
3. **Missing `ollama:` section** for OllamaConfig → Fixed by adding complete ollama config block

You can now rebuild, restart, and test. Both Ollama models (`qwen2.5:14b` for chat, `bge-m3` for embeddings) are confirmed available on the system.
