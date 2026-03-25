What can I do for you today?

I can help you inspect the repo, fix build issues, design the AI architecture, choose local models, wire Ollama into Candidate Match, and propose concrete code and config changes.

## What I looked at & why

I inspected these areas in the uploaded code to see how close you already are to a local-first setup:

* `candidate-match/.../infrastructure/integration/ollama/OllamaConfig.kt`
  to verify whether Ollama is already a first-class provider.
* `candidate-match/.../infrastructure/integration/ollama/OllamaHttpClient.kt`
  to see how chat/generation talks to Ollama.
* `candidate-match/.../infrastructure/integration/embedding/OllamaEmbeddingProvider.kt`
  to see how embeddings are produced locally.
* `candidate-match/.../infrastructure/integration/ai/AIContentGeneratorFactory.kt`
  to verify provider switching.
* `candidate-match/src/main/resources/application-local.yaml`
  to see the current local provider choices.
* `ai-rag-service/pom.xml` and Kotlin source files
  to diagnose the Spring AI / POM problem.

I also checked current official docs for Ollama and Spring AI, because model/starter names and supported versions change over time. Spring AI 1.0+ artifacts are now in Maven Central, and the official guidance is to use the Spring AI BOM together with provider/vector-store starters such as `spring-ai-starter-model-ollama` and `spring-ai-starter-vector-store-pgvector`. ([Home][1])

## Findings

* **Candidate Match is already close to local-first.** You already have:

    * `OllamaHttpClient` for text generation
    * `OllamaEmbeddingProvider` for `/api/embed`
    * `AIContentGeneratorFactory` with `OLLAMA`
    * local config with `embedding.provider: OLLAMA`

* **But local profile still routes key generation flows to Gemini.**
  In `application-local.yaml`, `ai.provider`, `matching.provider`, and `projectrequest.analysis.provider` are still effectively Gemini-driven, so you are not fully using the local stack yet.

* **Your current local model mix is okay for smoke tests, but weak for production-like evaluation.**
  `llama3.2:3b` is lightweight and fine for dev plumbing, but for CV analysis, requirement extraction, and customer-request interpretation, a stronger local model will give you much more realistic behavior. Ollama’s library shows `llama3.2:3b` is a small 3.21B / ~2GB model, while larger models like `qwen2.5:14b`, `gemma3:12b`, and `llama3.2-vision:11b` are available for stronger reasoning or multimodal use. ([ollama.com][2])

* **The AI RAG service POM is internally inconsistent.**
  It mixes:

    * property `spring-ai.version=1.0.2`
    * BOM import `1.1.0-M2`
    * explicit dependencies pinned to `1.1.0-M2`
    * non-starter/module-level artifacts

  That is a classic cause of Maven resolution and classpath mismatch issues.

* **The safest Spring AI fix is to align to one version and switch to starters.**
  Official Spring AI docs recommend the BOM plus starters, and PGVector/Ollama both have dedicated starter artifacts. ([Home][1])

## Recommendations (ranked)

1. **Make Ollama the default local provider for all dev AI flows** → you get zero-cost development with one switch → DIP / Clean Architecture.
2. **Use two local models, not one** → one embedding model + one stronger chat/reasoning model gives better quality and stability → SRP for model responsibilities.
3. **Keep `llama3.2:3b` only as a smoke-test model** → fast dev loop, but do not rely on it for realistic CV/project analysis → pragmatic quality separation.
4. **Promote a stronger local default such as `qwen2.5:14b` or `gemma3:12b`** → much better extraction, summarization, and ranking quality on your hardware → better fit for business use cases. ([ollama.com][3])
5. **Fix `ai-rag-service` by aligning Spring AI versions and using starters** → removes BOM drift and missing transitive config → OCP/Clean Architecture. ([Home][1])

## Design sketch

The clean split for your use case is:

* **Embedding model**

    * responsibility: vectorize CV chunks and customer-request text
    * recommended local default: `bge-m3`
    * optional lighter fallback: `embeddinggemma`
* **Reasoning/chat model**

    * responsibility: extract technologies, summarize CVs, explain strengths/weaknesses, draft customer responses
    * recommended local default: `qwen2.5:14b` or `gemma3:12b`
* **Optional vision model**

    * responsibility: only if you want to feed screenshots/scanned PDFs directly
    * recommended: `llama3.2-vision:11b`

Application side:

* `EmbeddingProvider` → local Ollama
* `AIContentGenerator` → local Ollama by default in `local`
* cloud providers remain as optional adapters for comparison or fallback

That keeps business use cases stable while infrastructure swaps between local and paid providers.

## What I recommend for your Mac

With 34 GB RAM and M3 Pro, I would run:

* **Embeddings:** `bge-m3`
* **Main local reasoning model:** `qwen2.5:14b`
* **Keep for quick smoke tests:** `llama3.2:3b`
* **Optional later:** `llama3.2-vision:11b` for image/PDF-like workflows

Why:

* `bge-m3` is a solid multilingual embedding model in Ollama and your code already expects its 1024-dim shape. ([ollama.com][4])
* `qwen2.5` supports multilingual workloads and up to 128K context according to Ollama’s library, which helps for long CVs and requirement docs. ([ollama.com][3])
* `gemma3` is also strong, multimodal, and available in 12B. ([ollama.com][5])
* `llama3.2:3b` is better kept as the “cheap dev sanity-check” model. ([ollama.com][2])

My default choice for you would be:

* **Local dev default:** `qwen2.5:14b`
* **Embeddings:** `bge-m3`
* **Fast fallback:** `llama3.2:3b`

## Code changes (diff-ready)

### 1) Make local profile truly local-first

`candidate-match/src/main/resources/application-local.yaml`

```yaml
embedding:
  enabled: true
  provider: OLLAMA
  model: bge-m3
  dimension: 1024

ollama:
  baseUrl: http://localhost:11434
  model: qwen2.5:14b
  embeddingModel: bge-m3
  embeddingFallbackModel: embeddinggemma

ai:
  provider: OLLAMA
  ollama:
    base-url: http://localhost:11434
    model: qwen2.5:14b

matching:
  provider: OLLAMA
  topN: 10
  enabled: true
  model: qwen2.5:14b

projectrequest:
  analysis:
    ai-enabled: true
    provider: OLLAMA

search:
  ollama:
    base-url: http://localhost:11434
    model: qwen2.5:14b
    connect-timeout-seconds: 5
    read-timeout-seconds: 120
    write-timeout-seconds: 30
```

### 2) Pull the models you actually need

```bash
ollama pull qwen2.5:14b
ollama pull bge-m3
ollama pull embeddinggemma
ollama pull llama3.2:3b
```

### 3) Improve `OllamaHttpClient` with chat endpoint support

Right now it uses `/api/generate`. For structured CV/requisition analysis, `/api/chat` is usually a better fit.

`candidate-match/.../infrastructure/integration/ollama/OllamaHttpClient.kt`

```kotlin
package no.cloudberries.candidatematch.infrastructure.integration.ollama

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.cloudberries.candidatematch.domain.ai.AIContentGenerator
import no.cloudberries.candidatematch.domain.ai.AIResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Service

@Service
class OllamaHttpClient(
    private val config: OllamaConfig
) : AIContentGenerator {

    private val mapper = jacksonObjectMapper()
    private val client = OkHttpClient()

    override fun generateContent(prompt: String): AIResponse {
        val payload = mapper.writeValueAsString(
            mapOf(
                "model" to config.model,
                "stream" to false,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to "You are a precise assistant for CV and project requirement analysis."),
                    mapOf("role" to "user", "content" to prompt)
                )
            )
        )

        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/api/chat")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) {
                "Ollama API error ${response.code}: ${response.message}"
            }

            val body = response.body?.string().orEmpty()
            val json = mapper.readTree(body)
            val content = json.path("message").path("content").asText()

            return AIResponse(
                content = content,
                modelUsed = config.model
            )
        }
    }
}
```

Change benefit: better role separation and more predictable prompt behavior.

## AI RAG service: likely root cause and fix

### What is wrong now

Your `ai-rag-service/pom.xml` currently mixes versions and module styles:

* property says `1.0.2`
* BOM imports `1.1.0-M2`
* some dependencies pin `1.1.0-M2`
* some are lower-level modules instead of starters

That is the first thing I would fix.

### Recommended POM shape

`ai-rag-service/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>no.cloudberries</groupId>
        <artifactId>cloudberries-candidate-match-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>..</relativePath>
    </parent>

    <groupId>no.cloudberries</groupId>
    <artifactId>ai-rag-service</artifactId>
    <name>AI RAG Service</name>

    <properties>
        <java.version>21</java.version>
        <kotlin.version>2.2.0</kotlin.version>
        <spring-boot.version>3.3.4</spring-boot.version>
        <spring-ai.version>1.0.2</spring-ai.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-ollama</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-advisors-vector-store</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-rag</artifactId>
        </dependency>

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-reflect</artifactId>
        </dependency>

        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
            <version>${kotlin.version}</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

Why this fix:

* single Spring AI version
* no milestone mixing
* official starters for Ollama and PGVector
* cleaner autoconfiguration path ([Home][1])

### Also add local RAG config

`ai-rag-service/src/main/resources/application-local.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/candidatematch
    username: candidatematch
    password: candidatematch123

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

server:
  port: 8082
```

## Tests

I could not run Maven in this environment because `mvn` is not installed here, so I am being careful: this is a source-level diagnosis, not a verified local build result.

The first tests I would run in your repo are:

1. **Ollama smoke test**

```bash
curl http://localhost:11434/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen2.5:14b",
    "messages": [{"role":"user","content":"Svar med OK"}],
    "stream": false
  }'
```

2. **Embedding smoke test**

```bash
curl http://localhost:11434/api/embed \
  -H "Content-Type: application/json" \
  -d '{
    "model": "bge-m3",
    "input": "Java Kotlin Spring Boot PostgreSQL"
  }'
```

3. **Candidate Match local profile**

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

4. **RAG service local profile**

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw -pl ai-rag-service spring-boot:run
```

## API contract

For long-term cleanliness, I would keep these responsibilities separate:

* `/api/v1/project-requests/analyze`
  extract technologies, must-have/should-have, role type, seniority
* `/api/v1/cvs/review`
  summarize strengths, weaknesses, gaps, rewrite suggestions
* `/api/v1/matches/preview`
  compare CV vs request and return score + rationale
* `/api/v1/rag/chat`
  retrieval-backed Q&A over embedded CVs/documents

That prevents one giant “do everything” prompt endpoint.

## DevEx & CI/CD

Local free setup I would use:

* Ollama on host at `11434`
* Open WebUI on `3000`
* Postgres + pgvector on `5433`
* Candidate Match on `8080`
* AI RAG service on `8082`

And I would define one local `.env` contract across services:

```env
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=qwen2.5:14b
OLLAMA_FAST_MODEL=llama3.2:3b
OLLAMA_EMBEDDING_MODEL=bge-m3
SPRING_PROFILES_ACTIVE=local
```

## Assumptions & next steps

Assumptions:

* I treated the uploaded zip snapshots as the current repo state.
* I could not execute Maven here, so the POM fix is reasoned from the dependency graph and official starter guidance.
* I assume your goal is **free local development by default**, with Gemini/OpenAI/Anthropic only for comparison or occasional validation.

My strongest recommendation is:

* keep **one embedding model** local at all times: `bge-m3`
* use **one stronger local reasoning model**: `qwen2.5:14b`
* keep `llama3.2:3b` only for smoke tests
* fix `ai-rag-service` by aligning Spring AI to one version and using starters

If you want, next I can turn this into a project-specific **`LOCAL_AI_SETUP.md`** for your repo, including exact env vars, Docker Compose, and a cleaned `ai-rag-service/pom.xml` patch.

[1]: https://docs.spring.io/spring-ai/reference/getting-started.html?utm_source=chatgpt.com "Getting Started :: Spring AI Reference"
[2]: https://ollama.com/library/llama3.2%3A3b?utm_source=chatgpt.com "llama3.2:3b - ollama.com"
[3]: https://ollama.com/library/qwen2.5%3A14b?utm_source=chatgpt.com "qwen2.5:14b - ollama.com"
[4]: https://ollama.com/library/bge-m3?utm_source=chatgpt.com "bge-m3 - Ollama"
[5]: https://www.ollama.com/library/gemma3?utm_source=chatgpt.com "gemma3 - ollama.com"
