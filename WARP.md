# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

This file guides WARP and developers through the **cloudberries-candidate-match** multi-module repository, covering architecture, setup, commands, APIs, embeddings/RAG, and troubleshooting for fast, reliable execution.

## Important Repository Rules

⚠️ **Critical setup requirements based on your user rules:**

- When writing backend code in `/candidate-match`, **always start DB first if not active**:
  ```bash
  docker-compose -f candidate-match/docker-compose-local.yaml up -d
  ```
- **Always use SDKMAN** to install Java and Maven:
  ```bash
  sdk install java 21.0.7-tem
  sdk use java 21.0.7-tem
  sdk install maven
  ```
- **For local development**, use **only username/password** DB auth - no certificate authentication
- The backend exposes an **OpenAPI contract**; frontend in `../cloudberries-candidate-match-web` uses generated types
- **After changing** `candidate-match/openapi.yaml`, copy to frontend and regenerate types:
  ```bash
  cp candidate-match/openapi.yaml ../cloudberries-candidate-match-web/openapi.yaml
  ```

## Repository Layout and Multi-Module Structure

**Parent Project (Root)**: Orchestrates modules via Maven, centralizes version management and shared plugin configuration.

**Modules**:
- **`candidate-match`** (port 8080): Primary backend service exposing candidate matching, project request domain, and core business APIs
- **`ai-rag-service`** (port 8081): Retrieval-augmented generation, embeddings, semantic search, and vector operations using Spring AI
- **`teknologi-barometer-service`** (port 8082): Analytics and insights service with Gmail integration and technology barometer functionality

**Multi-Module Maven Commands**:
```bash
# Build all modules
mvn -T 1C clean package

# Run specific module with dependencies
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local

# Test specific module
mvn -pl ai-rag-service -am test
```

## Architecture Overview and Key Concepts

### Service Architecture
- **Candidate Match**: Core business logic, consultant/project matching, exposes REST APIs and OpenAPI documentation
- **AI RAG Service**: 
  - Ingestion and chunking of CV/job posting text
  - Embedding generation via OpenAI/Gemini, stores vectors in PostgreSQL via pgvector
  - Semantic similarity search using cosine distance
- **Teknologi Barometer**: Gmail integration, technology analytics, connects to candidate-match for consultant matching

### Data Layer
- **PostgreSQL** with **pgvector extension** stores:
  - Relational entities (consultants, skills, projects, CVs)
  - Vector embeddings for semantic search
  - Separate databases per service where configured
- **Database migrations**: Uses **Liquibase** (confirmed from application configs)

### Key Concepts
- **pgvector**: PostgreSQL extension for storing vectors and index-accelerated similarity search
- **Embeddings**: Fixed-length numeric vectors representing text content for semantic search
- **RAG Pipeline**: Retrieve semantically similar chunks, then pass context to LLM for generation
- **Hybrid Search**: Combines structural filters with semantic ranking for optimal results

### Semantic vs Structural Search

**Structural Search**:
- Traditional SQL filters on structured fields (skills, experience, location)
- Deterministic, explainable, fast for exact constraints
- Misses semantic similarity beyond exact matches

**Semantic Search**:
- Converts text into embeddings, uses pgvector similarity
- Finds conceptually similar content even with different terminology
- Requires embedding model, vector storage, careful context design

**This Project's Approach**:
1. Apply structural filters first (narrow by hard constraints)
2. Use semantic search on remaining candidates (rank by conceptual fit)
3. RAG pipeline synthesizes insights using retrieved context

## Prerequisites and Toolchain

**Required Tools**:
```bash
# SDKMAN (required)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Java 21 Temurin
sdk install java 21.0.7-tem
sdk use java 21.0.7-tem

# Maven via SDKMAN
sdk install maven

# Docker and Docker Compose (macOS)
brew install --cask docker
# Docker Compose is included with Docker Desktop
```

**Verification**:
```bash
java -version
mvn -v
docker -v
docker-compose -v
```

**Optional**: Node.js for frontend integration with `../cloudberries-candidate-match-web`

## Environment Variables

**Copy and configure environment variables**:
```bash
# Use the existing .env as template (contains placeholder values)
cp .env .env.local
# Edit .env.local with your actual API keys
```

**Key Variables** (from .env and application configs):

**External APIs**:
- `FLOWCASE_API_KEY`: Flowcase API integration
- `FLOWCASE_BASE_URL`: https://cloudberries.flowcase.com/api
- `OPENAI_API_KEY`: OpenAI for embeddings/chat (ai-rag-service, teknologi-barometer)
- `GEMINI_API_KEY`: Google Gemini for embeddings/chat (candidate-match)

**Database** (candidate-match local profile):
- `POSTGRES_USER=candidatematch`
- `POSTGRES_PASSWORD=candidatematch123`
- Database: `candidatematch` on `localhost:5433`

**AI RAG Service Database** (requires env vars):
- `CM_DB_URL`: Connection to shared candidate-match database
- `CM_DB_USERNAME`: Database username
- `CM_DB_PASSWORD`: Database password

**Optional** (teknologi-barometer):
- `GMAIL_*`: Gmail API integration variables
- `CANDIDATE_MATCH_URL=http://localhost:8080`

## Local Database Setup with Docker Compose

**Start PostgreSQL with pgvector**:
```bash
# From repository root
cd candidate-match
docker-compose -f docker-compose-local.yaml up -d

# Verify status
docker-compose -f docker-compose-local.yaml ps
docker-compose -f docker-compose-local.yaml logs -f postgres-local
```

**Database Configuration**:
- **Image**: `pgvector/pgvector:pg15` (pgvector pre-installed)
- **Container**: `cloudberries-postgres-local`
- **Port**: `5433:5432` (avoids conflicts)
- **Database**: `candidatematch`
- **Users**: `postgres/postgres123` (container), `candidatematch/candidatematch123` (app)

**Verify pgvector**:
```bash
psql "host=localhost port=5433 dbname=candidatematch user=candidatematch password=candidatematch123"

# In psql:
CREATE EXTENSION IF NOT EXISTS vector;
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
```

**Database Migrations**:
- Liquibase runs automatically on Spring Boot startup
- Manual migration: Liquibase changesets in `src/main/resources/db/changelog/`
- pgvector context activated in local profile

## Running the Services (Local Development)

**1. Start Database**:
```bash
cd candidate-match
docker-compose -f docker-compose-local.yaml up -d
```

**2. Build All Modules**:
```bash
mvn -T 1C -DskipTests clean package
```

**3. Run Services**:

**Candidate Match** (primary service, port 8080):
```bash
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local

# With debug:
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

**AI RAG Service** (port 8081):
```bash
# Requires CM_DB_* environment variables set
export CM_DB_URL="jdbc:postgresql://localhost:5433/candidatematch"
export CM_DB_USERNAME="candidatematch"
export CM_DB_PASSWORD="candidatematch123"

mvn -pl ai-rag-service -am spring-boot:run
```

**Teknologi Barometer Service** (port 8082):
```bash
# Uses separate database on port 5434 by default
mvn -pl teknologi-barometer-service -am spring-boot:run
```

**4. Access Points**:
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html` (candidate-match)
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **Health**: `http://localhost:8080/actuator/health`

## API Reference (Comprehensive List)

### Candidate Match Service (Port 8080)

**Core Endpoints**:
- `GET /api/consultants` - List consultants with pagination
- `POST /api/consultants/search` - Structural search with filters
- `POST /api/consultants/search/semantic` - Semantic search using embeddings
- `GET /api/consultants/search/embedding-info` - Embedding configuration info
- `GET /api/consultants/with-cv` - Consultants with normalized CV data

**Project Requests**:
- `GET /api/project-requests` - List stored project requests
- `POST /api/project-requests/upload` - Upload and analyze PDF
- `GET /api/project-requests/{id}` - Get project request by ID

**Matching**:
- `POST /api/matches` - Find matches from project description text
- `POST /api/matches/upload` - Upload CV (PDF) and find matches

**Skills**:
- `GET /api/skills` - List skills with consultant counts

**CV Operations**:
- `GET /api/cv/{userId}` - Get CV data (JSON) for user
- `GET /api/cv-score/{candidateId}` - Get CV score for candidate
- `POST /api/cv-score/{candidateId}/run` - Run CV scoring
- `POST /api/cv-score/run/all` - Score all candidates
- `GET /api/cv-score/all` - List all candidates overview

**Embeddings**:
- `POST /api/embeddings/run/jason` - Demo: generate Jason's embeddings
- `POST /api/embeddings/run?userId=X&cvId=Y` - Generate specific embeddings
- `POST /api/embeddings/run/missing?batchSize=100` - Batch process missing

**Sync & Health**:
- `POST /api/consultants/sync/run` - Sync from Flowcase
- `GET /api/health` - Health check (aggregated)
- `POST /api/chatbot/analyze` - AI content analysis

### AI RAG Service (Port 8081)

**Configuration from application.yml**:
- Vector store table: `vector_store`
- Chunking: 400 max tokens, 50 overlap tokens
- Ingestion SQL configurable for CV text extraction

### Teknologi Barometer Service (Port 8082)

**Configuration Features**:
- Gmail integration for technology trend analysis
- Candidate matching via candidate-match-url
- Scheduled ingestion and aggregation
- Prometheus metrics exposure

## Embedding Generation Workflows

**Ingestion and Chunking** (ai-rag-service):
- Splits CV/job posting text using configured chunk size (400 tokens) and overlap (50 tokens)
- Normalizes to plain text, strips HTML/markup
- Configurable SQL query for data extraction

**Embedding Generation**:
- Uses OpenAI `text-embedding-3-small` or Gemini `text-embedding-004`
- Stores vectors in PostgreSQL using pgvector extension
- COSINE_DISTANCE for similarity calculations

**Available Workflows**:
```bash
# Manual embedding generation (candidate-match)
curl -X POST http://localhost:8080/api/embeddings/run/jason
curl -X POST "http://localhost:8080/api/embeddings/run?userId=thomas&cvId=andersen"
curl -X POST "http://localhost:8080/api/embeddings/run/missing?batchSize=100"

# Semantic search
curl -X POST http://localhost:8080/api/consultants/search/semantic \
  -H "Content-Type: application/json" \
  -d '{"text": "Senior Kotlin developer", "topK": 5}'
```

**Requirements for Embeddings**:
- `embedding.enabled=true` in local profile (candidate-match)
- `GEMINI_API_KEY` or `OPENAI_API_KEY` set
- pgvector extension installed in database

## Health Checks, Metrics, and Monitoring

**Health Endpoints**:
```bash
# Candidate Match
curl -s http://localhost:8080/actuator/health | jq
curl -s http://localhost:8080/actuator/health/readiness | jq

# Teknologi Barometer (more endpoints exposed)
curl -s http://localhost:8082/actuator/health | jq
curl -s http://localhost:8082/actuator/metrics | jq
curl -s http://localhost:8082/actuator/prometheus
```

**Health Check Components**:
- Database connectivity
- Flowcase API availability
- GenAI service availability (Gemini/OpenAI)
- pgvector extension status

## Testing Structure and Commands

**Test Categories**:
- **Unit Tests** (Surefire): `*Test.kt`, fast execution, no external dependencies
- **Integration Tests** (Failsafe): `*IT.kt` or `*IntegrationTest.kt`, uses Testcontainers

**Test Commands**:
```bash
# Unit tests only (fast)
mvn -q -DskipITs=true clean test

# Integration tests only (requires Docker)
mvn -q -DskipTests=true -DskipITs=false clean verify

# All tests
mvn -q clean verify

# pgvector integration tests (special flag)
mvn -q -DskipITs=false -DrunPgVectorIT=true clean verify

# Module-specific tests
mvn -pl candidate-match -am test
```

## Common Commands and Workflows

**Daily Development**:
```bash
# Start local environment
cd candidate-match && docker-compose -f docker-compose-local.yaml up -d

# Build and run main service
mvn -T 1C clean package
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local

# Quick tests
mvn -q -DskipITs=true clean test

# Check API endpoints
open http://localhost:8080/swagger-ui/index.html

# Generate embeddings
curl -X POST http://localhost:8080/api/embeddings/run/jason
```

**Database Operations**:
```bash
# Connect to local database
psql "host=localhost port=5433 dbname=candidatematch user=candidatematch password=candidatematch123"

# Check migrations
docker-compose -f candidate-match/docker-compose-local.yaml logs postgres-local
```

## Frontend Integration and OpenAPI Sync

**OpenAPI Contract Synchronization**:
```bash
# When candidate-match/openapi.yaml changes:
cp candidate-match/openapi.yaml ../cloudberries-candidate-match-web/openapi.yaml

# Generate from running service:
curl -s http://localhost:8080/v3/api-docs.yaml > ../cloudberries-candidate-match-web/openapi.yaml

# Regenerate frontend types (in frontend repo):
npm run gen:api
```

## Troubleshooting

**Database Issues**:
```bash
# Container not starting
docker-compose -f candidate-match/docker-compose-local.yaml logs postgres-local
lsof -i :5433  # Check port conflicts

# Connection failures
psql "host=localhost port=5433 dbname=candidatematch user=candidatematch password=candidatematch123"

# pgvector not found
psql -c "CREATE EXTENSION IF NOT EXISTS vector;" # Ensure extension is installed
```

**API/Service Issues**:
```bash
# Missing API keys
echo $GEMINI_API_KEY  # Verify environment variables are set

# Health check failures
curl http://localhost:8080/actuator/health

# OpenAPI not updating
cp candidate-match/openapi.yaml ../cloudberries-candidate-match-web/openapi.yaml
```

**Common Local Development Issues**:
- **Port conflicts**: Postgres uses 5433, adjust if needed
- **Embeddings not working**: Verify API keys and `embedding.enabled=true`
- **Health check errors in tests**: Expected in isolated test contexts
- **Rosetta required**: On Apple Silicon for some test containers
- **Certificate auth**: Use username/password only for local development

**Legacy Notes from Original WARP.md**:
- Embedded Postgres in tests may require Rosetta 2 on Apple Silicon
- Health check errors in test logs are expected in isolation
- Maven plugins use `useModulePath=false` to avoid classpath issues
