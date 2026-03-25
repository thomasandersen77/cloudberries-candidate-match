# Ollama Integration Troubleshooting Guide

## Issue Summary

**Error**: `java.net.SocketTimeoutException: timeout` when calling `/api/cv-score/{candidateId}/run`

**Root Cause**: The `OllamaHttpClient` was using default OkHttp timeouts (10 seconds), which is insufficient for CV scoring operations that require LLM inference time.

## Solution Applied

### Code Changes

#### 1. Updated `OllamaConfig.kt`
Added timeout configuration properties:

```kotlin path=/Users/tandersen/git/cloudberries-candidate-match/ai-rag-service/src/main/kotlin/no/cloudberries/ai/config/OllamaConfig.kt start=6
@Component
@ConfigurationProperties(prefix = "ollama")
class OllamaConfig {
    var baseUrl: String = "http://localhost:11434"
    var model: String = "deepseek-r1:8b"
    var connectTimeoutSeconds: Long = 10      // NEW
    var readTimeoutSeconds: Long = 120        // NEW
    var writeTimeoutSeconds: Long = 30        // NEW
}
```

#### 2. Updated `OllamaHttpClient.kt`
Configured OkHttpClient to use timeout settings:

```kotlin path=/Users/tandersen/git/cloudberries-candidate-match/ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/integration/ollama/OllamaHttpClient.kt start=24
private val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
        .build()
}
```

#### 3. Updated `application-local.yaml`
Added timeout configuration and changed model to `qwen2.5:14b` for better CV scoring:

```yaml path=/Users/tandersen/git/cloudberries-candidate-match/candidate-match/src/main/resources/application-local.yaml start=87
ollama:
  baseUrl: http://localhost:11434
  model: qwen2.5:14b                        # chat/generate model for CV scoring
  embeddingModel: bge-m3                    # embedding model
  embeddingFallbackModel: nomic-embed-text  # fallback embedding if bge-m3 is unavailable
  connectTimeoutSeconds: 10                 # connection timeout
  readTimeoutSeconds: 180                   # read timeout (3 minutes for CV scoring)
  writeTimeoutSeconds: 30                   # write timeout
```

## Verification Steps

### 1. Verify Ollama is Running

```bash
# Check if Ollama is running
pgrep -fl ollama

# Expected output should include:
# - ollama serve (main process)
# - ollama runner (when model is loaded)
```

### 2. Verify Required Models are Available

```bash
# List all available models
curl -s http://localhost:11434/api/tags | jq '.models[].name'

# Required models:
# - qwen2.5:14b (or qwen2.5:14b-instruct)
# - bge-m3:latest
# - llama3.2:3b (optional, for lightweight operations)
```

### 3. Pull Missing Models (if needed)

```bash
# Pull qwen2.5:14b for CV scoring
ollama pull qwen2.5:14b

# Pull bge-m3 for embeddings
ollama pull bge-m3

# Pull llama3.2:3b for lightweight operations
ollama pull llama3.2:3b
```

### 4. Test Ollama Connection

```bash
# Test API connectivity
curl -s http://localhost:11434/api/version

# Test model generation
curl -s http://localhost:11434/api/generate -d '{
  "model": "qwen2.5:14b",
  "prompt": "Say hello",
  "stream": false
}' | jq '.response'
```

### 5. Rebuild and Restart Application

```bash
# From project root
mvn clean package -DskipTests

# Run with local profile
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local
```

### 6. Test CV Scoring Endpoint

```bash
# Replace {candidateId} with actual ID from database
curl -X POST http://localhost:8080/api/cv-score/{candidateId}/run

# Expected: Success response with CV score
# If timeout occurs, check Ollama logs and model performance
```

## Model Configuration Overview

| Model | Purpose | Size | Config Location |
|-------|---------|------|-----------------|
| `qwen2.5:14b` | CV scoring, content generation | 14.8B params (Q4_K_M) | `ollama.model` |
| `bge-m3` | Embeddings for semantic search | 566M params (F16) | `ollama.embeddingModel` |
| `llama3.2:3b` | Lightweight chat/testing | 3.2B params (Q4_K_M) | Fallback/testing |

## Timeout Configuration Rationale

- **Connect Timeout (10s)**: Sufficient for local Ollama connection
- **Read Timeout (180s)**: CV scoring requires LLM inference time, especially for larger models
- **Write Timeout (30s)**: Adequate for sending prompts/payloads

## Common Issues

### Issue 1: Connection Refused
**Symptom**: `java.net.ConnectException: Failed to connect to localhost:11434`

**Solutions**:
```bash
# Start Ollama service
ollama serve

# Or on macOS with Homebrew service
brew services start ollama
```

### Issue 2: Model Not Found
**Symptom**: Error message indicating model doesn't exist

**Solutions**:
```bash
# Pull the required model
ollama pull qwen2.5:14b

# Verify it's available
ollama list
```

### Issue 3: Timeout After 180 Seconds
**Symptom**: Request times out even with 180s read timeout

**Solutions**:
1. Use a smaller/faster model (e.g., `qwen2.5:7b` or `llama3.2:3b`)
2. Increase `readTimeoutSeconds` in config
3. Check Ollama resource usage (CPU/RAM)
4. Consider GPU acceleration if available

### Issue 4: IPv6 Connection Attempts
**Symptom**: Suppressed exception showing `Failed to connect to localhost/[0:0:0:0:0:0:0:1]:11434`

**Solutions**:
- This is OkHttp trying IPv6 first, then falling back to IPv4
- Add JVM argument to prefer IPv4: `-Djava.net.preferIPv4Stack=true`
- Or update `/etc/hosts` to ensure `localhost` resolves correctly:
  ```
  127.0.0.1   localhost
  ::1         localhost
  ```

## Performance Optimization

### Model Selection Trade-offs

| Model | Inference Speed | Quality | Memory Usage |
|-------|----------------|---------|--------------|
| `llama3.2:3b` | ⚡⚡⚡ Fast | ⭐⭐ Good | ~2 GB |
| `qwen2.5:14b` | ⚡⚡ Moderate | ⭐⭐⭐ Excellent | ~9 GB |
| `gemma3:12b` | ⚡⚡ Moderate | ⭐⭐⭐ Excellent | ~8 GB |

### Recommended Configuration by Use Case

**Fast Development/Testing**:
```yaml
ollama:
  model: llama3.2:3b
  readTimeoutSeconds: 60
```

**Production Quality CV Scoring**:
```yaml
ollama:
  model: qwen2.5:14b
  readTimeoutSeconds: 180
```

**High Volume Processing**:
- Consider using Gemini API (`ai.provider: GEMINI`) for batch operations
- Or deploy Ollama with GPU acceleration
- Or use model queue/pool pattern

## Monitoring

### Check Ollama Logs

```bash
# If running via Homebrew service
brew services info ollama
tail -f ~/Library/Logs/Homebrew/ollama/ollama.log

# If running manually
# Logs will be in the terminal where ollama serve is running
```

### Application Logs

Look for these log entries:
- `OllamaHttpClient` connection attempts
- `TimerAspect` method timing information
- `ScoreCandidateService` scoring progress

```bash
# In application logs
grep -i "ollama" logs/application.log
grep -i "performCvScoring" logs/application.log
```

## Alternative: Switch to Cloud Provider

If local Ollama performance is insufficient, switch to Gemini:

```yaml
ai:
  provider: GEMINI  # Change from OLLAMA

projectrequest:
  analysis:
    provider: GEMINI  # Change from OLLAMA
```

Ensure `GEMINI_API_KEY` is set in your environment.

## Next Steps

1. ✅ Code changes applied
2. ⏭️ Rebuild application: `mvn clean package`
3. ⏭️ Restart with local profile
4. ⏭️ Test CV scoring endpoint
5. ⏭️ Monitor logs for timeout issues
6. ⏭️ Adjust timeout values if needed based on actual inference times

## References

- [Ollama API Documentation](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [OkHttp Timeouts](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/read-timeout/)
- [Project WARP.md](./WARP.md) - Complete repository guide
