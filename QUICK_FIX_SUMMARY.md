# Quick Fix Summary: Ollama Timeout Issue

## Problem
`java.net.SocketTimeoutException: timeout` after 10 seconds when calling CV scoring endpoint.

## Root Cause
`OllamaHttpClient` had no timeout configuration, using OkHttp's default 10-second timeout - insufficient for LLM inference.

## Solution: 3 File Changes

### 1. `ai-rag-service/src/main/kotlin/no/cloudberries/ai/config/OllamaConfig.kt`
```kotlin
@Component
@ConfigurationProperties(prefix = "ollama")
class OllamaConfig {
    var baseUrl: String = "http://localhost:11434"
    var model: String = "deepseek-r1:8b"
    var connectTimeoutSeconds: Long = 10      // ✅ ADDED
    var readTimeoutSeconds: Long = 120        // ✅ ADDED
    var writeTimeoutSeconds: Long = 30        // ✅ ADDED
}
```

### 2. `ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/integration/ollama/OllamaHttpClient.kt`
```kotlin
// Add import
import java.util.concurrent.TimeUnit

// Update client builder
private val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)  // ✅ ADDED
        .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)        // ✅ ADDED
        .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)      // ✅ ADDED
        .build()
}
```

### 3. `candidate-match/src/main/resources/application-local.yaml`
```yaml
ollama:
  baseUrl: http://localhost:11434
  model: qwen2.5:14b                        # ✅ CHANGED from llama3.2:3b
  embeddingModel: bge-m3
  embeddingFallbackModel: nomic-embed-text
  connectTimeoutSeconds: 10                 # ✅ ADDED
  readTimeoutSeconds: 180                   # ✅ ADDED (3 minutes for CV scoring)
  writeTimeoutSeconds: 30                   # ✅ ADDED
```

## Verification Status

✅ Ollama is running on port 11434  
✅ Required models are available:
  - `qwen2.5:14b` (chat/generation for CV scoring)
  - `bge-m3:latest` (embeddings)
  - `llama3.2:3b` (lightweight fallback)

## Next Steps to Test

```bash
# 1. Rebuild
mvn clean package -DskipTests

# 2. Restart application
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local

# 3. Test CV scoring (replace {candidateId} with actual ID)
curl -X POST http://localhost:8080/api/cv-score/{candidateId}/run
```

## Expected Outcome

- CV scoring should complete within 180 seconds
- No more `SocketTimeoutException` errors
- Application logs should show successful Ollama communication

## Fallback: If Still Times Out

If the `qwen2.5:14b` model is too slow for your hardware:

**Option 1**: Use faster model
```yaml
ollama:
  model: llama3.2:3b  # Much faster, lower quality
  readTimeoutSeconds: 60
```

**Option 2**: Switch to cloud provider
```yaml
ai:
  provider: GEMINI  # Instead of OLLAMA
projectrequest:
  analysis:
    provider: GEMINI
```

## For IntelliJ Jetbrains AI

The code changes have been applied. You can now:
1. Review the changes in the three modified files
2. Rebuild and restart the application
3. Test the CV scoring endpoint
4. Refer to `OLLAMA_TROUBLESHOOTING.md` for detailed diagnostics

All required Ollama models are confirmed available on the system.
