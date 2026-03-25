# Provider Routing Fix: OpenAI 404 Error

## Problem Summary

**Error**: `java.lang.RuntimeException: Feil fra OpenAI API: 404`  
**Endpoint**: `/api/cv-score/{candidateId}/run`  
**Expected**: Should use Ollama for local CV scoring  
**Actual**: Application was routing to OpenAI instead of Ollama

## Root Cause Analysis

### The Issue
The `AISettings` class reads configuration from the `ai` prefix:

```kotlin path=/Users/tandersen/git/cloudberries-candidate-match/ai-rag-service/src/main/kotlin/no/cloudberries/ai/config/AISettings.kt start=8
@ConfigurationProperties(prefix = "ai")
class AISettings {
    var enabled: Boolean = true
    var timeout: Duration = Duration.ofSeconds(30)
    var provider: AIProvider = AIProvider.OPENAI  // ⚠️ Default is OPENAI
    var fallbackEnabled: Boolean = true
}
```

### Configuration Hierarchy Problem

The `application-local.yaml` had the provider setting under `spring.ai.provider` instead of top-level `ai.provider`:

**❌ BEFORE (Incorrect - under spring.ai)**:
```yaml
spring:
  ai:
    provider: OLLAMA  # ❌ This is spring.ai.provider, not ai.provider!
```

**✅ AFTER (Correct - top-level)**:
```yaml
ai:
  provider: OLLAMA  # ✅ This is ai.provider, correctly loaded by AISettings
```

### Call Stack Analysis

When CV scoring is triggered:
1. `CvScoreController.runScoreForCandidate()` → no provider specified
2. `CvScoreAppService.scoreCandidate(candidateId, aiProvider = null)` → null passed
3. `ScoreCandidateService.performCvScoring(aiProvider = null, ...)` → null passed
4. `AIContentAnalysisService.analyzeContent(content, provider = null)` → null passed
5. `AiContentGenerationAdapter.generateContent(prompt, provider = null)`:
   ```kotlin
   val selectedProvider = provider ?: aiSettings.provider  // ⚠️ Falls back to aiSettings.provider
   ```
6. Since `aiSettings.provider` wasn't being set (configuration under wrong path), it used the default: `AIProvider.OPENAI`
7. `OpenAIHttpClient.generateContent()` → **404 Error** (no valid OpenAI API key/assistant)

## Solution Applied

### 1. Restructured `application-local.yaml`

**Added top-level `ollama:` section** for `OllamaConfig`:
```yaml path=/Users/tandersen/git/cloudberries-candidate-match/candidate-match/src/main/resources/application-local.yaml start=47
ollama:
  baseUrl: http://localhost:11434
  model: qwen2.5:14b                        # chat/generate model for CV scoring
  embeddingModel: bge-m3                    # embedding model
  embeddingFallbackModel: nomic-embed-text  # fallback embedding
  connectTimeoutSeconds: 10                 # connection timeout
  readTimeoutSeconds: 180                   # read timeout (3 minutes for CV scoring)
  writeTimeoutSeconds: 30                   # write timeout
```

**Added top-level `ai:` section** for `AISettings`:
```yaml path=/Users/tandersen/git/cloudberries-candidate-match/candidate-match/src/main/resources/application-local.yaml start=56
ai:
  provider: OLLAMA  # ✅ Now correctly loaded by AISettings
  timeout: 180s
  models:
    interpretation: gemini-2.5-flash
    generation_default: gemini-2.5-flash
    generation_quality: gemini-2.5-pro
    embeddings: bge-m3
```

**Kept `spring.ai.ollama`** for Spring AI framework:
```yaml path=/Users/tandersen/git/cloudberries-candidate-match/candidate-match/src/main/resources/application-local.yaml start=32
spring:
  ai:
    ollama:
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
```

### 2. Configuration Sections Explained

| Section | Purpose | Consumed By |
|---------|---------|-------------|
| `spring.ai.ollama` | Spring AI framework Ollama integration | Spring AI Auto-configuration |
| `ollama` | OllamaHttpClient connection settings | `OllamaConfig` (`@ConfigurationProperties(prefix="ollama")`) |
| `ai` | Provider selection and general AI settings | `AISettings` (`@ConfigurationProperties(prefix="ai")`) |
| `search.ollama` | Search-specific Ollama configuration | Search service (legacy) |

### 3. Earlier Timeout Fix (Still Applied)

From the previous fix, we also updated `OllamaConfig` and `OllamaHttpClient`:

```kotlin path=/Users/tandersen/git/cloudberries-candidate-match/ai-rag-service/src/main/kotlin/no/cloudberries/ai/config/OllamaConfig.kt start=6
@Component
@ConfigurationProperties(prefix = "ollama")
class OllamaConfig {
    var baseUrl: String = "http://localhost:11434"
    var model: String = "deepseek-r1:8b"
    var connectTimeoutSeconds: Long = 10
    var readTimeoutSeconds: Long = 120
    var writeTimeoutSeconds: Long = 30
}
```

```kotlin path=/Users/tandersen/git/cloudberries-candidate-match/ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/integration/ollama/OllamaHttpClient.kt start=24
private val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
        .build()
}
```

## Verification Steps

### 1. Verify Configuration Hierarchy

```bash
# Check that ai.provider is at top level, not under spring.ai
grep -A 1 "^ai:" candidate-match/src/main/resources/application-local.yaml

# Expected output:
# ai:
#   provider: OLLAMA
```

### 2. Rebuild Application

```bash
mvn clean package -DskipTests
```

### 3. Restart with Local Profile

```bash
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. Verify Provider Selection in Logs

Look for log entries showing Ollama connection attempts, not OpenAI:

```bash
# Good: Should see OllamaHttpClient activity
grep -i "ollama" logs/application.log

# Bad: Should NOT see OpenAI 404 errors
grep -i "openai.*404" logs/application.log
```

### 5. Test CV Scoring Endpoint

```bash
# Replace {candidateId} with actual ID
curl -X POST http://localhost:8080/api/cv-score/{candidateId}/run

# Expected: Success (or timeout if model is slow, but NOT 404)
# Success response will include:
# - scorePercent
# - summary
# - strengths
# - potentialImprovements
```

### 6. Verify Ollama is Being Used

Check application startup logs for:
```
AISettings loaded: provider=OLLAMA
OllamaConfig loaded: baseUrl=http://localhost:11434, model=qwen2.5:14b
```

## Expected Behavior After Fix

| Scenario | Provider Used | Expected Result |
|----------|--------------|-----------------|
| `/api/cv-score/{id}/run` (no provider param) | Ollama (`qwen2.5:14b`) | ✅ CV scoring succeeds |
| `/api/consultants/search/semantic` | Ollama (`bge-m3` embeddings) | ✅ Semantic search works |
| Project request analysis | Ollama | ✅ Analysis completes |
| Explicit `provider=GEMINI` param | Gemini | ✅ Uses Gemini API |

## Troubleshooting

### Still Getting OpenAI 404 Error?

**Check 1: Configuration loaded correctly**
```bash
# Start application with debug logging
mvn -pl candidate-match -am spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.arguments="--logging.level.no.cloudberries.ai=DEBUG"

# Look for AISettings initialization
grep "AISettings" logs/application.log
```

**Check 2: YAML syntax**
```bash
# Validate YAML structure (Python required)
python3 -c "import yaml; yaml.safe_load(open('candidate-match/src/main/resources/application-local.yaml'))"

# Should not produce errors
```

**Check 3: Spring Boot profile active**
```bash
# Verify local profile is active
curl http://localhost:8080/actuator/health | jq '.profiles'

# Expected: ["local"]
```

### Ollama Still Timing Out?

Refer to `OLLAMA_TROUBLESHOOTING.md` for comprehensive Ollama-specific diagnostics.

### Need to Switch Back to Gemini?

```yaml
# In application-local.yaml
ai:
  provider: GEMINI  # Change from OLLAMA to GEMINI
```

Ensure `GEMINI_API_KEY` is set in environment.

## Key Learnings

### Spring Boot Configuration Binding

1. **@ConfigurationProperties prefix must match YAML top-level key**:
   - `@ConfigurationProperties(prefix = "ai")` → reads from `ai:` in YAML
   - `@ConfigurationProperties(prefix = "ollama")` → reads from `ollama:` in YAML

2. **Spring AI uses `spring.ai` namespace**:
   - `spring.ai.ollama` is for Spring AI framework auto-configuration
   - Custom application properties should NOT be under `spring.*`

3. **Configuration precedence**:
   - Environment variables > Command-line args > application-{profile}.yaml > application.yaml > Defaults in code

### Multi-Provider Architecture

The application supports multiple AI providers through the adapter pattern:
- `AiContentGenerationPort` (interface)
- `AiContentGenerationAdapter` (implementation, routes to providers)
- Provider-specific HTTP clients (OpenAI, Gemini, Ollama, Anthropic)

Selection happens at runtime based on:
1. Explicit `provider` parameter passed to methods
2. `AISettings.provider` configuration (fallback)

## Files Modified

1. `ai-rag-service/src/main/kotlin/no/cloudberries/ai/config/OllamaConfig.kt` - Added timeout properties
2. `ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/integration/ollama/OllamaHttpClient.kt` - Applied timeouts
3. `candidate-match/src/main/resources/application-local.yaml` - Restructured AI configuration

## Next Steps

- ✅ Configuration fixed
- ✅ Ollama timeouts configured
- ⏭️ Rebuild: `mvn clean package -DskipTests`
- ⏭️ Restart: `mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local`
- ⏭️ Test: `curl -X POST http://localhost:8080/api/cv-score/{candidateId}/run`
- ⏭️ Monitor logs for successful Ollama usage

## References

- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties)
- [Spring AI Ollama Documentation](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- [OLLAMA_TROUBLESHOOTING.md](./OLLAMA_TROUBLESHOOTING.md) - Ollama-specific diagnostics
- [WARP.md](./WARP.md) - Complete repository guide
