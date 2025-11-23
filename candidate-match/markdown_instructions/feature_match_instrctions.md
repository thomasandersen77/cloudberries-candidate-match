**Feature: Match Candidate for Customer — End-to-End Implementation Guide (Backend + OpenAPI + Frontend Sync + Tests)**

This document instructs WARP on how to implement, verify, and ship the “Match Candidate for Customer” feature end-to-end in the cloudberries-candidate-match repository. It is designed to be followed step-by-step and references existing APIs, code structure, environment rules, OpenAPI contracts, frontend sync, testing, and troubleshooting.

## Read me first:
* Always run the candidate-match module with Java 21.0.7-tem via SDKMAN to avoid bytebuddy conflicts.
* Always start the local DB with Docker Compose before coding or testing.
* Base path: spring.mvc.servlet.path=/api — all endpoints below assume /api.


### 1) Feature Goals, Acceptance Criteria, and End-User Flow


Goals
## Enable a user to:
- View/select an existing Project Request stored in candidate-match.
- Trigger candidate matching using either:
- The selected Project Request text, or
- Raw text (job description) or an uploaded PDF.
- Receive a ranked list (Top K) of best-matching consultants.
* Persist each match request and its results for observability and later retrieval/audit.
* Support hybrid ranking combining structural filters, semantic similarity, and (optionally) CV quality.
* Provide robust local dev setup, curl verification, OpenAPI updates, frontend type regeneration, and automated tests.

## Acceptance Criteria 
### Backend
- Existing API endpoints are used for:
- Listing Project Requests: GET /api/project-requests
- Triggering matching from text: POST /api/matches
- Triggering matching from PDF: POST /api/matches/upload
- Retrieving top results: Returned directly as the response from POST /api/matches (and POST /api/matches/upload)
- Matching returns Top K consultants with scores and ranking.
- Matching logic runs reliably; work is persisted (match request + results) and can be inspected in the DB.
- OpenAPI spec is current and synced to the frontend types.
### Frontend integration
- Frontend can call the endpoints, pass topK, and optionally a forced mode, and render the returned Top K list.
- After updating the OpenAPI spec, the types in ../cloudberries-candidate-match-web are regenerated.
### Tests
- Unit tests validate ranking, weighting, and request parsing.
- Integration tests validate end-to-end behavior with a real DB (Testcontainers or local Docker in dev).
### Ops/devx
- Clear troubleshooting guidance (DB, ports, API keys, ByteBuddy/JDK, Liquibase).

### End-User Flow (from the web app perspective)
1. User selects a Project Request (or uploads a PDF / pastes text).
2. User chooses Top K (default 3) and optionally a forced search mode (structural, semantic, hybrid).
3. User clicks “Find Matches.”
4. Backend returns a ranked list of consultants with scores (and optional reasons/highlights).
5. User views the top candidates and may drill down.


## 2) Existing Backend API Endpoints to Use (Base path: /api)


Important
* spring.mvc.servlet.path=/api is configured; all routes below include /api.
* Service port: 8080 (candidate-match).

Already existing endpoints relevant for this feature:
### Project Requests
-GET /api/project-requests
- Lists stored project requests.
-GET /api/project-requests/{id}
- Returns details for a specific project request. 
### Matching
- POST /api/matches
- Finds matches from project description text (and/or a referenced Project Request).
- POST /api/matches/upload
- Uploads a PDF (e.g., a job spec) and finds matches.

### Retrieving Top Results
* The Top K results are returned directly in the response from POST /api/matches (and POST /api/matches/upload). No extra read endpoint is required to retrieve Top K for this feature scope.

Request/Response Contracts (current behavior and expectations)
* POST /api/matches
- Content-Type: application/json
- Request body (controller supports matching by either projectRequestId or raw text):
{
"projectRequestId": "optional-string-id",
"text": "optional free-text job description",
"topK": 3,
"forcedMode": "hybrid",
"includeReasoning": true
}
Notes:
* Provide either projectRequestId or text (if both provided, projectRequestId typically takes precedence).
* topK defaults to 3 if omitted.
* forcedMode is optional; allowed values depend on code (commonly structural | semantic | hybrid).
* includeReasoning is optional; if supported, it returns debug explanations/highlights.
* 200 OK (synchronous match) response example:
{
"matchRequestId": "3d9a5f1e-....",
"topK": 3,
"mode": "hybrid",
"startedAt": "2025-10-25T12:34:56Z",
"durationMs": 245,
"results": [
{
"rank": 1,
"consultantId": "c-123",
"consultantName": "Jane Doe",
"cvId": "cv-789",
"score": 0.91,
"semanticScore": 0.88,
"skillScore": 0.93,
"cvQualityScore": 0.80,
"highlights": ["Kotlin", "Spring Boot", "AWS"],
"reasons": "Strong Kotlin/Spring background with recent AWS projects"
}
]
}

#### POST /api/matches/upload
- Content-Type: multipart/form-data
- Form fields:
- file: the PDF to upload
- topK: optional integer (default 3)
- forcedMode: optional string
- includeReasoning: optional boolean
-200 OK response matches the shape of POST /api/matches.
* GET /api/project-requests
- 200 OK example:
[
{
"id": "pr-001",
"title": "Backend Kotlin Developer",
"createdAt": "2025-10-24T10:11:12Z",
"source": "uploaded",
"textSummary": "Looking for Kotlin/Spring, AWS, CI/CD"
}
]

* GET /api/project-requests/{id}
- 200 OK example:
{
"id": "pr-001",
"title": "Backend Kotlin Developer",
"createdAt": "2025-10-24T10:11:12Z",
"source": "uploaded",
"text": "Full job description text...",
"metadata": {...}
}

Supporting endpoints (useful during verification, not required for this feature)
#### POST /api/consultants/search/semantic
- Can be used to sanity-check semantic ranking.
#### Health and OpenAPI
- GET /actuator/health
- GET /v3/api-docs
- Swagger UI: http://localhost:8080/swagger-ui/index.html


### 3) How Matching Works in the Current Code (Architecture Overview)


#### High-level pipeline
1. Input acquisition
   - From projectRequestId: Load stored text for the request.
   - From text: Use the provided raw description.
   - From PDF: Extract text server-side (via PDF parsing) and treat as raw text.
2. Candidate set narrowing (structural/keyword filters, optional)
   - Use existing consultant and skills repositories to narrow down the candidate set (e.g., skills, years, location).
3. Semantic ranking
   - Generate/query embeddings and compute semantic similarity between the project text and consultant CVs/embeddings (pgvector).
   - Endpoint /api/consultants/search/semantic is available for debugging; main matching integrates similar logic internally.
4. CV quality signal (optional)
   - Retrieve precomputed CV quality scores (if available) via cv- score endpoints or internal service.
5. Score aggregation
   - Combine signals into a single score:
   score = αsemantic + βskills + γ*cvQuality (weights configurable in code or properties).
6. Persistence
   - Persist the match request (parameters, timestamps) and each result row with component scores and rank.
7. Response
   - Return the Top K results in the response (synchronous), or expose status/results if async behavior is configured.

Typical components in the codebase (names may vary; search with ripgrep ag/rg or IDE):
#### Entities (JPA)
- ProjectRequestEntity (id, title, text, createdAt, source, ...)
- MatchRequestEntity (id, projectRequestId?, mode, topK, status, createdAt, updatedAt, rawText?)
- MatchResultEntity (id, matchRequestId, consultantId, cvId, score, semanticScore, skillScore, cvQualityScore, rank, reasons JSONB?)
- ConsultantEntity, SkillEntity, CV/Embedding entities (existing domain)
#### Repositories
- ProjectRequestRepository
- MatchRequestRepository
- MatchResultRepository
- ConsultantRepository, SkillRepository
#### Services
- MatchingService (entry point orchestrating structural filtering, semantic ranking, and aggregation)
- SemanticSearchService or EmbeddingSearchService (pgvector queries)
- CvScoreService (optional) for CV quality signals
- DocumentProcessingService (PDF text extraction for /api/matches/upload)
#### Async processing
- Matching may use Spring @Async or a TaskExecutor for compute- intensive flows while the controller still returns a synchronous result for typical sizes.
- Regardless of sync/async, requests and results are persisted for auditability.
#### Persistence notes
- Tables: match_request, match_result (confirm Liquibase changelogs)
- Recommended: Ensure ON DELETE CASCADE on match_result(match_request_id) to simplify cleanup.
- Liquibase runs automatically on app startup.


### 4) Local Setup Prerequisites (Exact, per repo rules)

Install and use SDKMAN, Java 21.0.7- tem, Maven (via SDKMAN)
* This is mandatory to avoid ByteBuddy issues.
* Commands:
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.7-tem
sdk use java 21.0.7-tem
sdk install maven

Start local Postgres with pgvector (Docker Compose)
* From repository root:
cd candidate-match
docker-compose -f docker-compose-local.yaml up -d
* Verify:
docker-compose -f docker-compose-local.yaml ps
docker-compose -f docker-compose-local.yaml logs -f postgres-local

Environment variables
* Copy .env to .env.local and customize:
cp .env .env.local
* Ensure .env.local is in .gitignore and not committed.
* Local DB defaults (from repo configs):
POSTGRES_USER=candidatematch
POSTGRES_PASSWORD=candidatematch123
DB host: localhost:5433, DB: candidatematch
* GenAI keys (if semantic search/embeddings are used):
GEMINI_API_KEY and/or OPENAI_API_KEY

Build and run service (local profile)
* From repository root:
mvn -T 1C clean package
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local
* Swagger UI:
open http://localhost:8080/swagger-ui/index.html

Note: For local development, use username/password DB auth only (no certificate auth).

Optional tooling
* Node.js for frontend type regeneration
* .NET SDK (installed on this Mac as per user environment) — not required by candidate-match but available.


### 5) Manual Verification with curl (Happy Path)


List project requests
curl -s http://localhost:8080/api/project-requests | jq

Get a specific project request
curl -s http://localhost:8080/api/project-requests/pr-001 | jq

Trigger matching from text (Top K = 3)
curl -s -X POST http://localhost:8080/api/matches \
-H "Content-Type: application/json" \
-d '{
"text": "We need a Senior Kotlin developer with Spring Boot and AWS experience",
"topK": 3,
"forcedMode": "hybrid",
"includeReasoning": true
}' | jq

Trigger matching from a known Project Request
curl -s -X POST http://localhost:8080/api/matches \
-H "Content-Type: application/json" \
-d '{
"projectRequestId": "pr-001",
"topK": 5
}' | jq

Trigger matching by uploading a PDF (multipart)
curl -s -X POST http://localhost:8080/api/matches/upload \
-F "file=@/path/to/job-spec.pdf" \
-F "topK=5" | jq

Optional: sanity-check semantics directly
curl -s -X POST http://localhost:8080/api/consultants/search/semantic \
-H "Content-Type: application/json" \
-d '{"text": "Senior Kotlin developer", "topK": 5}' | jq


6) OpenAPI Update and Frontend Sync


When changing the API (request/response fields or documentation):
1. Update backend OpenAPI/YAML (candidate-match/openapi.yaml) for:
   -POST /api/matches (request supports projectRequestId or text; topK; forcedMode; includeReasoning)
   -POST /api/matches/upload (multipart form-data; topK; forcedMode; includeReasoning)
   -GET /api/project-requests and GET /api/project-requests/{id}
2. Copy the OpenAPI spec to the frontend repository:
   cp candidate-match/openapi.yaml ../cloudberries-candidate-match-web/openapi.yaml
   Alternatively, generate from the running service:
   curl -s http://localhost:8080/v3/api-docs.yaml > ../cloudberries-candidate-match-web/openapi.yaml
3. Regenerate frontend types in ../cloudberries-candidate-match-web:
   cd ../cloudberries-candidate-match-web
   npm run gen:api
4. Verify that the frontend:
   -Reads and persists Top K and forced mode preferences (localStorage) per existing UI rules.
   -Sends the chosen topK and forcedMode values to POST /api/matches or /api/matches/upload.

Note: The frontend repository sits at ../cloudberries-candidate-match-web relative to this repo.


### 7) Testing Expectations (Unit + Integration)


Unit tests (candidate-match)
* Scope
-Score aggregation: verify αsemantic + βskills + γ* cvQuality combines correctly and sorts in descending order.
-Request parsing: validate behavior when only projectRequestId is provided vs only text; topK defaulting.
-Forced mode routing: ensure structural, semantic, hybrid logic paths behave as expected.
-PDF extraction: unit-test extraction component with small sample PDFs (mocked where appropriate).
*Conventions and commands
-Name tests *Test.kt (Surefire).
-Run fast unit tests:
mvn -q -DskipITs=true clean test

Integration tests
* Use Testcontainers for PostgreSQL with pgvector or run against the local Docker DB in CI/dev (Testcontainers preferred).
* Seed minimal consultant data and skills; optionally seed embeddings if semantic path is tested.
* Post to /api/matches and assert:
- 200 OK
- results size == topK
- scores sorted descending
- persistence: match_request and match_result contain the expected records
* Commands:
- Integration tests only:
mvn -q -DskipTests=true -DskipITs=false clean verify
*All tests:
mvn -q clean verify

Minimum test matrix
* Text-only request with default topK (3).
* projectRequestId path with explicit topK (5).
* forcedMode = structural, semantic, hybrid.
* PDF upload path.
* Empty/invalid inputs return 4xx with clear messages.


### 8) Troubleshooting


Common issues
* Java/ByteBuddy conflicts
- Always use Java 21.0.7-tem via SDKMAN:
sdk use java 21.0.7-tem
* Database not reachable or port conflicts
- Start DB:
docker-compose -f candidate-match/docker-compose-local.yaml up -d
* Check logs:
docker-compose -f candidate-match/docker-compose-local.yaml logs -f postgres-local
* Verify psql connectivity:
psql "host=localhost port=5433 dbname=candidatematch user=candidatematch password=candidatematch123"
* Liquibase migration failures
- Ensure pgvector extension exists:
CREATE EXTENSION IF NOT EXISTS vector;
* Check changelog order and foreign key constraints.
* Semantic search not working
- Ensure GEMINI_API_KEY and/or OPENAI_API_KEY set in environment (or .env.local).
- Confirm embedding.enabled=true (if applicable).
* Wrong base path or 404s
- Remember spring.mvc.servlet.path=/api. Example: POST http://localhost:8080/api/matches
* OpenAPI/UI not matching runtime
- Refresh Swagger at:
http://localhost:8080/swagger-ui/index.html
* Copy updated openapi.yaml to frontend and regenerate types.
* CORS issues when testing frontend
- Validate CORS config on candidate-match.
* Local auth
- Use username/password for DB; do not use certificate auth locally.

Health and diagnostics
* Health:
curl -s http://localhost:8080/actuator/health | jq
* OpenAPI JSON:
curl -s http://localhost:8080/v3/api-docs | jq


### 9) Implementation Notes and Code Navigation


* Packages to inspect (names may vary):
- com.cloudberries.candidatematch.matching (controllers/services)
- com.cloudberries.candidatematch.projectrequest (entities/repositories)
- com.cloudberries.candidatematch.consultant (consultant/skills domain)
- com.cloudberries.candidatematch.embedding 
*Look for:
- MatchingService (entry point): matchFromText, matchFromProjectRequest
- SemanticSearchService/EmbeddingSearchService (pgvector queries)
- CvScoreService (optional signal)
- Repositories: ProjectRequestRepository, MatchRequestRepository, MatchResultRepository, ConsultantRepository
*Persistence
- Ensure match_request and match_result tables exist; results should reference request via FK.
- Consider ON DELETE CASCADE on match_result(match_request_id) to simplify cleanup.
*Async processing
- Matching may offload heavy compute via @Async, but controller returns results synchronously for normal payloads.
- Persist intermediary status if needed (status: RUNNING, COMPLETED, FAILED).


### 10) Assumptions and Open Questions


Assumptions (align with product/tech leads):
* Top K default is 3; configurable via request topK.
* Hybrid ranking uses a weighted sum of semantic, skills, and CV quality; default weights are set in configuration.
* POST /api/matches and /api/matches/upload return Top K directly (synchronous) and also persist to DB.
* PDF parsing is handled server-side upon upload; the extracted text is not persisted unless part of a Project Request.

Open questions (decide and codify if not already):
* Should Top K be globally configurable via application properties in addition to request-level topK?
* Exact default weights for score aggregation (α, β, γ) and whether they are environment-configurable.
* Whether to expose an optional GET /api/matches/{matchRequestId} for polling/reviewing historic results (if async behavior is expanded).
* Should CV quality be optional/toggleable per request?
* Do we want to expose explainability fields (reasons/highlights) by default, or behind includeReasoning=true?
* Confirm the exact PDF size/type limits and error handling for /api/matches/upload.


Appendix: Useful Commands


Start DB:
cd candidate-match
docker-compose -f docker-compose-local.yaml up -d

Run candidate-match (local):
sdk use java 21.0.7-tem
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local

Build all:
mvn -T 1C clean package

Copy OpenAPI to frontend and regenerate:
cp candidate-match/openapi.yaml ../cloudberries-candidate-match-web/openapi.yaml
cd ../cloudberries-candidate-match-web
npm run gen:api

Context I used (for accuracy and alignment):
* candidate-match/markdown_instructions/feature_match_candidat_for_customer.md (original content and intent)
* Spring base path: candidate-match/src/main/resources/application.yaml (spring.mvc.servlet.path=/api)
* OpenAPI contract (candidate-match/openapi.yaml)
* Liquibase changelog (candidate-match/src/main/resources/db/changelog/db.changelog-master.xml)
* Matching implementation and services (e.g. candidate-match/src/main/kotlin/.../service/matching, ai/semantic search orchestration)
* API overview docs (candidate-match/API-ENDPOINTS.md)