# WARP.md

This file explains how to work effectively in this repository after the modular-monolith refactor.

Use this document as the primary repo guide for WARP and other LLMs. If older markdown files disagree with this file, trust the current source tree and runtime wiring first.

## Read this first

- Use SDKMAN and Java `21.0.7-tem` for this repo.
- Start PostgreSQL before backend work:
  ```bash
  docker-compose -f candidate-match/docker-compose-local.yaml up -d
  ```
- Local database auth is username/password only. Do not use certificate auth for local development.
- `candidate-match` is the main executable backend.
- `ai-rag-service` is currently a dependency module that contributes beans to the main app. It is not a separately launched service in the checked-in refactor state.
- Keep provider-specific LLM and embedding code in `ai-rag-service`.
- Keep business logic, domain rules, persistence, and REST use cases in `candidate-match`.
- Keep cross-module AI interfaces in `ai-platform-contracts`.
- If `candidate-match/openapi.yaml` changes, sync it to `../cloudberries-candidate-match-web/openapi.yaml` and regenerate frontend types.

## Repository mental model

```text
cloudberries-candidate-match/
├── pom.xml                         # parent pom / module orchestrator
├── ai-platform-contracts/          # shared AI ports, DTOs, provider enums
├── ai-rag-service/                 # provider adapters, Spring AI RAG, LLM clients
├── candidate-match/                # main Spring Boot app, business logic, DB, REST API
└── teknologi-barometer-service/    # separate bounded context / separate app
```

The intended dependency direction is:

```text
candidate-match
  ├── depends on ai-platform-contracts
  └── depends on ai-rag-service

ai-rag-service
  └── depends on ai-platform-contracts
```

The important architectural idea is:
- `candidate-match` decides what the business flow is.
- `ai-platform-contracts` defines how the core asks for AI work.
- `ai-rag-service` decides which provider performs that AI work.

## What actually boots

The main backend starts from `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/Main.kt`.

That class effectively loads beans from:
- `no.cloudberries.candidatematch`
- `no.cloudberries.ai`

It also still contains a stale `no.cloudberries.teknologibarometer` scan entry, but the checked-in barometer module uses `no.cloudberries.barometer`, so treat barometer as a separate app unless you intentionally wire it into the main runtime.

That means:
- one Spring Boot process starts on port `8080`
- `candidate-match` beans are loaded
- `ai-rag-service` beans are also loaded into the same process
- `/api/rag/**` endpoints can exist even though their controller lives in `ai-rag-service`

Do not assume `ai-rag-service` is a separately running app on port `8081`. Older docs say that, but the current checked-in state does not include a standalone boot application or checked-in `application.yml` for `ai-rag-service`.

## Module responsibilities

### `candidate-match`

This is the core business module and main web application.

It owns:
- REST controllers
- application services and business orchestration
- domain entities and rules
- JPA entities and repositories
- Liquibase migrations
- Flowcase integration
- project request workflows
- consultant search workflows
- CV scoring workflows
- match result persistence

It may still contain AI-oriented application services, but those should remain provider-agnostic and talk only through ports.

Important files and areas:
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/Main.kt`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/projectrequest/`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/consultants/`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/ai/`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/matches/`
- `candidate-match/src/main/resources/application.yaml`
- `candidate-match/src/main/resources/application-local.yaml`
- `candidate-match/src/main/resources/db/changelog/`

Examples of core services that depend on ports rather than provider clients:
- `service/projectrequest/ProjectRequestAnalysisService.kt`
- `service/ai/AIQueryInterpretationService.kt`
- `service/ai/AIContentAnalysisService.kt`
- `service/ai/RAGContextService.kt`
- `service/consultants/ConsultantSearchService.kt`
- `service/matching/CandidateMatchingService.kt`

### `ai-platform-contracts`

This is the anti-corruption boundary between the core module and the AI integration module.

It contains shared ports and types such as:
- `AiContentGenerationPort`
- `QueryInterpretationPort`
- `ProjectRequestAnalysisPort`
- `EmbeddingPort`
- `CandidateMatchingPort`
- `AIProvider`
- `AIResponse`
- `CandidateMatchResponse`
- query interpretation DTOs and other shared AI-facing types

When the core needs a new AI capability, add or evolve the contract here first.

### `ai-rag-service`

This module should own communication with LLMs and embedding providers.

It currently contains:
- adapter implementations for shared AI ports
- provider-specific HTTP clients for Gemini, OpenAI, Anthropic, and Ollama
- Spring AI based generic RAG components
- prompt templates related to AI interpretation / analysis / matching

Important files and areas:
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/ai/`
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/integration/`
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/rag/`
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/config/`

Key implementations:
- `AiContentGenerationAdapter.kt`
- `QueryInterpretationAdapter.kt`
- `ProjectRequestAnalysisAdapter.kt`
- `infrastructure/integration/gemini/GeminiHttpClient.kt`
- `infrastructure/integration/openai/OpenAIHttpClient.kt`
- `infrastructure/integration/anthropic/AnthropicHttpClient.kt`
- `infrastructure/integration/ollama/OllamaHttpClient.kt`
- `rag/api/RagController.kt`
- `rag/service/RagService.kt`
- `rag/service/DbIngestionService.kt`

### `teknologi-barometer-service`

Treat this as a separate bounded context and separate app.

It is not the main candidate-match runtime. Keep it conceptually separate from the monolith work unless the task explicitly involves barometer functionality.

## The intended AI architecture

Use this three-layer mental model:

1. `candidate-match` decides the business use case
   - analyze a project request
   - interpret a chat query
   - score a CV
   - find consultants
   - persist match results

2. `ai-platform-contracts` defines the stable API between core and AI infrastructure
   - generate content
   - interpret search intent
   - analyze request text
   - generate embeddings
   - compare CV vs request

3. `ai-rag-service` implements those contracts against real providers
   - Gemini
   - OpenAI
   - Anthropic
   - Ollama
   - Spring AI `ChatClient` and `VectorStore`

When refactoring or adding code:
- do not import provider clients directly into `candidate-match`
- do not make `ai-rag-service` depend on `candidate-match`
- keep domain models in `candidate-match`
- prefer raw text or shared AI DTOs across the module boundary

## Current refactor status and important caveats

The refactor is real, but the codebase is still mid-transition. Keep these caveats in mind.

### 1. AI orchestration still exists in `candidate-match`

Some AI-facing application services still live in `candidate-match`, for example:
- `service/ai/AIQueryInterpretationService.kt`
- `service/ai/AIContentAnalysisService.kt`
- `service/ai/RAGService.kt`
- `service/ai/RAGContextService.kt`
- `service/ai/AISearchOrchestrator.kt`

That is acceptable as long as they remain provider-agnostic and talk through ports.

Do not add raw Gemini/OpenAI/Ollama HTTP logic to these classes.

### 2. There are two vector / RAG paths today

There are effectively two AI retrieval stacks in the repo right now.

#### Candidate-match custom semantic / chunk flow
Uses:
- `EmbeddingPort`
- `CvEmbeddingService`
- `ConsultantSearchService`
- `CvEmbeddingRepository`
- `CvChunkEmbeddingRepository`
- `RAGContextService`

Tables involved:
- `cv_embedding`
- `cv_chunk_embedding`

#### Spring AI generic RAG flow from `ai-rag-service`
Uses:
- `rag/api/RagController.kt`
- `rag/service/RagService.kt`
- `rag/service/DbIngestionService.kt`
- `rag/service/StartupIngestRunner.kt`
- Spring AI `VectorStore`

Table involved:
- `vector_store`

Do not mix these up when debugging semantic search, ingestion, or consultant chat behavior.

### 3. Local profile is Ollama-oriented, but not fully local-only yet

`candidate-match/src/main/resources/application-local.yaml` clearly points toward local Ollama development, but some generation-related settings still default to Gemini.

Notable local settings:
- `spring.ai.ollama.chat.options.model: qwen2.5:14b`
- `spring.ai.ollama.embedding.options.model: bge-m3`
- `embedding.provider: OLLAMA`
- `embedding.model: bge-m3`
- `ollama.model: llama3.2:3b`
- `search.ollama.model: llama3.2:1b`
- `ai.provider: GEMINI`
- `projectrequest.analysis.provider: GEMINI`
- `matching.provider: GEMINI`

Interpretation:
- the local-development intention is Postgres + pgvector + Ollama
- some business flows are still configured to use Gemini unless you explicitly align them
- when making the monolith more local-first, update the provider selection through ports and configuration, not by bypassing the architecture

### 4. Embedding wiring is still worth double-checking

The checked-in source currently contains `GoogleGeminiEmbeddingProvider` as the visible `EmbeddingPort` implementation in `ai-rag-service`.

At the same time, `application-local.yaml` declares:
- `embedding.provider: OLLAMA`
- `embedding.model: bge-m3`
- `embedding.dimension: 1024`

Treat this as refactor-in-progress. If semantic search or embedding generation behaves strangely under the local profile, inspect the actual active bean and code path before assuming Postgres or Ollama is broken.

### 5. Old Gemini Files API docs are not the source of truth

There are still markdown files describing older Gemini Files API matching flows.

Current source includes legacy comments and placeholder exceptions in `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/matches/service/ProjectMatchingServiceImpl.kt`.

If you need to work on matching:
- verify the current code path first
- do not assume old markdown docs are still accurate
- do not reintroduce removed Gemini-specific coupling into the core module

## Local development workflow

### Prerequisites

Use SDKMAN and the repo-local toolchain.

Fastest path:
```bash
sdk env
```

Or explicitly:
```bash
sdk use java 21.0.7-tem
```

Install Maven with SDKMAN if needed:
```bash
sdk install maven
```

The repo-local `.sdkmanrc` currently declares:
- `java=21.0.7-tem`
- `maven=3.9.9`

### Start PostgreSQL with pgvector

```bash
docker-compose -f candidate-match/docker-compose-local.yaml up -d
```

Useful checks:
```bash
docker-compose -f candidate-match/docker-compose-local.yaml ps
docker-compose -f candidate-match/docker-compose-local.yaml logs -f postgres-local
psql "host=localhost port=5433 dbname=candidatematch user=candidatematch password=candidatematch123"
```

### Run the main backend

```bash
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local
```

### Build everything

```bash
mvn -T 1C clean package
```

### Fast unit-test loop

```bash
mvn -q -DskipITs=true clean test
```

### Integration tests

```bash
mvn -q -DskipTests=true -DskipITs=false clean verify
```

### Useful local URLs

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`
- Health: `http://localhost:8080/actuator/health`
- Generic RAG API: `http://localhost:8080/api/rag/**`

## Local Postgres details

Local database source of truth:
- file: `candidate-match/docker-compose-local.yaml`
- container: `cloudberries-postgres-local`
- image: `pgvector/pgvector:pg15`
- exposed port: `5433`
- database: `candidatematch`
- username: `candidatematch`
- password: `candidatematch123`

Liquibase source of truth:
- `candidate-match/src/main/resources/db/changelog/db.changelog-master.xml`

Local Liquibase contexts:
- `default`
- `pgvector`

AI-related changelog milestones include:
- embeddings
- AI RAG tables
- conversation tables
- match result tables
- 1024-dimension embedding adjustment
- Spring AI vector store support

## Effective configuration locations

The main runtime configuration for the monolith lives in `candidate-match/src/main/resources/`.

Important files:
- `application.yaml`
- `application-local.yaml`
- `application-prod.yaml`

Important configuration groups:
- `spring.datasource.*`
- `spring.ai.ollama.*`
- `spring.ai.vectorstore.pgvector.*`
- `flowcase.*`
- `gemini.*`
- `openai.*`
- `anthropic.*`
- `ollama.*`
- `embedding.*`
- `ai.*`
- `rag.ingest.*`
- `search.ollama.*`

`ai-rag-service` currently relies on the host application for configuration. Do not waste time looking for a checked-in standalone `ai-rag-service/src/main/resources/application.yml` in the current refactor state.

## Ollama for local development

The repo already contains local Ollama helpers.

Relevant files:
- `scripts/ollama-chat`
- `scripts/ollama-embed`
- `scripts/ollama_local_app.py`
- `ollama/Modelfile`
- `ollama/Modelfile.gemma3-12b`
- `ollama/Modelfile.embeddinggemma-300m`

### Recommended local model split

Use separate local models for different responsibilities:
- reasoning / semantic understanding of CVs and customer requests: `qwen2.5:14b`
- embeddings: `bge-m3`
- optional smoke-test model: `llama3.2:3b`
- optional stronger alternative: `gemma3:12b`

### Quick Ollama smoke test

```bash
python3 scripts/ollama_local_app.py --self-test
```

### Local chat REPL

```bash
scripts/ollama-chat
```

### Local embedding test

```bash
scripts/ollama-embed "Senior Kotlin developer with Spring Boot and PostgreSQL"
```

### Important nuance

There are multiple Ollama-related config groups because different code paths use different abstractions:
- Spring AI generic RAG uses `spring.ai.ollama.*`
- the direct Ollama HTTP client uses `ollama.*`
- some search-related helper settings use `search.ollama.*`

When a feature does not use the model you expect, identify the code path first.

## Search, embeddings, and consultant chat

### Semantic search

Primary business-facing files:
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/consultants/ConsultantSearchService.kt`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/embedding/CvEmbeddingService.kt`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/infrastructure/repositories/embedding/CvEmbeddingRepository.kt`

Semantics:
- embeddings are generated through `EmbeddingPort`
- semantic search is backed by pgvector in PostgreSQL
- the service validates provider/model compatibility before executing semantic search

### Consultant-specific RAG chat

Primary files:
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/ai/RAGService.kt`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/ai/RAGContextService.kt`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/ai/AISearchOrchestrator.kt`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/ai/ConversationService.kt`

Semantics:
- chunks can be generated and stored in `cv_chunk_embedding`
- follow-up chat context is persisted in conversation tables
- the orchestrator chooses between structured, semantic, hybrid, and RAG flows

## Generic Spring AI RAG endpoints

These come from `ai-rag-service`, but they run inside the main candidate-match process because of component scanning.

Source files:
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/rag/api/RagController.kt`
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/rag/service/RagService.kt`
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/rag/service/DbIngestionService.kt`
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/rag/service/StartupIngestRunner.kt`

Endpoints:
- `POST /api/rag/chat`
- `POST /api/rag/ingest`
- `POST /api/rag/ingest/db`

Important defaults from `application.yaml`:
- `rag.ingest.on-start: false`
- DB ingestion SQL is configurable under `rag.ingest.sql`
- chunking defaults to `400` max tokens and `50` overlap tokens

## Project request analysis flow

Current intended split:
- `candidate-match` owns upload, PDF text extraction, persistence, and domain flow
- `ai-platform-contracts` defines the analysis contract
- `ai-rag-service` implements the provider-backed analysis

Key files:
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/projectrequest/ProjectRequestAnalysisService.kt`
- `ai-platform-contracts/src/main/kotlin/no/cloudberries/ai/port/ProjectRequestAnalysisPort.kt`
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/ai/ProjectRequestAnalysisAdapter.kt`

If you refactor this flow further, move provider details toward `ai-rag-service`, not the other way around.

## Query interpretation and AI search orchestration

Key files:
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/ai/AIQueryInterpretationService.kt`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/ai/AISearchOrchestrator.kt`
- `ai-platform-contracts/src/main/kotlin/no/cloudberries/ai/port/QueryInterpretationPort.kt`
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/ai/QueryInterpretationAdapter.kt`

Mental model:
- candidate-match interprets the business/search consequence of a user query
- ai-rag-service supplies the LLM-backed interpretation through the port
- candidate-match then executes structured, semantic, hybrid, or RAG behavior

## OpenAPI and frontend sync

Frontend repo:
- `../cloudberries-candidate-match-web`

When the backend contract changes:
```bash
cp candidate-match/openapi.yaml ../cloudberries-candidate-match-web/openapi.yaml
npm --prefix ../cloudberries-candidate-match-web run gen:api
```

Or export from the running service:
```bash
curl -s http://localhost:8080/v3/api-docs.yaml > ../cloudberries-candidate-match-web/openapi.yaml
npm --prefix ../cloudberries-candidate-match-web run gen:api
```

Do not assume the root-level `openapi.yaml` is the only canonical contract. Verify whether the change belongs in `candidate-match/openapi.yaml` or should be exported from the running backend.

## Known local-development gotchas

- `sync.consultants.on-startup: true` in `application-local.yaml` means the app may call Flowcase on startup.
- local RAG and semantic behavior can differ depending on whether the path uses Spring AI or the custom embedding port flow.
- if semantic search fails with provider/model mismatch, inspect `ConsultantSearchService` and the active `EmbeddingPort` bean.
- if `/api/rag/**` is missing, inspect `Main.kt` component scanning and the `ai-rag-service` module dependency.
- if someone refers to a standalone `ai-rag-service` process, verify that they are not working from stale documentation.
- keep real credentials out of tracked files; use local environment-specific configuration for actual secrets.

## High-value file map

Start here when exploring the repo:
- `pom.xml`
- `candidate-match/pom.xml`
- `ai-rag-service/pom.xml`
- `ai-platform-contracts/pom.xml`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/Main.kt`
- `candidate-match/src/main/resources/application.yaml`
- `candidate-match/src/main/resources/application-local.yaml`
- `candidate-match/src/main/resources/db/changelog/db.changelog-master.xml`
- `ai-platform-contracts/src/main/kotlin/no/cloudberries/ai/port/`
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/ai/`
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/integration/`
- `ai-rag-service/src/main/kotlin/no/cloudberries/ai/rag/`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/ai/`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/consultants/`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/projectrequest/`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/embedding/`
- `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/matches/`

## Default assumptions if the repo feels inconsistent

Assume all of the following unless the current code proves otherwise:
- the target architecture is a modular monolith
- `candidate-match` is the main executable and business core
- `ai-rag-service` is the AI integration module and should own provider communication
- `ai-platform-contracts` is the stable boundary between them
- local development is intended to use Postgres + pgvector + Ollama
- some markdown docs still describe pre-refactor or partially completed states
- the current source tree and actual bean wiring are more trustworthy than old documentation

