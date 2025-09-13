# Cloudberries Candidate Match

End-to-end candidate matching service integrating with Flowcase for CV data, Google Gemini for content generation and
embeddings, and PostgreSQL/pgvector for vector storage. This README includes quick-start instructions, test guidance,
configuration for local development on macOS (zsh), and API endpoints for triggering embeddings on-demand.

## Architecture
```mermaid
graph TB
    subgraph Frontend["Web/Client Layer"]
        CLI["Web Client/Scheduler"]
    end

    subgraph Application["Application Layer"]
        MS["Matching Service"]
        FS["Flowcase Service"]
        AS["AI Service"]
    end

    subgraph Integration["Integration Layer"]
        FC["Flowcase Client"]
        OAI["OpenAI Client"]
        GEM["Gemini Client"]
    end

    subgraph External["External Services"]
        FAPI["Flowcase API"]
        OAPI["OpenAI API"]
        GAPI["Google Gemini API"]
    end

    subgraph Domain["Domain Layer"]
        CAN["Candidate"]
        PROJ["Project"]
        SKILL["Skills"]
        MATCH["Match Results"]
    end

    %% Connections
    CLI --> MS
    CLI --> FS
    
    MS --> AS
    MS --> FS
    
    FS --> FC
    AS --> OAI
    AS --> GEM
    
    FC --> FAPI
    OAI --> OAPI
    GEM --> GAPI
    
    MS --> CAN
    MS --> PROJ
    MS --> SKILL
    MS --> MATCH

    %% Styling
    classDef service fill:#f9f,stroke:#333,stroke-width:2px
    classDef client fill:#bbf,stroke:#333,stroke-width:2px
    classDef external fill:#fbb,stroke:#333,stroke-width:2px
    classDef domain fill:#bfb,stroke:#333,stroke-width:2px
    
    class MS,FS,AS service
    class FC,OAI,GEM client
    class FAPI,OAPI,GAPI external
    class CAN,PROJ,SKILL,MATCH domain
```

## Prerequisites (macOS, zsh)

- Java 21 (install via SDKMAN)
- Maven (install via SDKMAN) — the project also includes the Maven Wrapper (mvnw)
- Docker (for running PostgreSQL or Testcontainers-based integration tests)

Install SDKMAN, Java, and Maven:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.3-tem
sdk install maven
```

## Configuration

Application properties are loaded from environment variables (preferred) or application YAML files. The main keys:

- FLOWCASE_API_KEY
- FLOWCASE_BASE_URL
- OPENAI_API_KEY
- OPENAI_ASSISTANT_ID
- GEMINI_API_KEY

Embedding configuration (defaults in application.yaml):

- embedding.enabled: false (enable in local profile)
- embedding.provider: GEMINI
- embedding.model: text-embedding-004
- embedding.dimension: 768

Local profile (application-local.yaml) also configures Liquibase context "pgvector" and a local Postgres URL:

- spring.datasource.url: jdbc:postgresql://localhost:5433/candidatematch
- spring.datasource.username: ${POSTGRES_USER}
- spring.datasource.password: ${POSTGRES_PASSWORD}

Export env vars in zsh (add to ~/.zshrc for persistence):

```bash
export FLOWCASE_API_KEY={{your_flowcase_api_key}}
export FLOWCASE_BASE_URL={{your_flowcase_base_url}}
export OPENAI_API_KEY={{your_openai_api_key}}
export OPENAI_ASSISTANT_ID={{your_openai_assistant_id}}
export GEMINI_API_KEY={{your_gemini_api_key}}
export POSTGRES_USER=candidatematch
export POSTGRES_PASSWORD=candidatematch123
```

## Database (local)

The local profile expects PostgreSQL on localhost:5433. Options:

- Use Docker directly:

```bash
docker run --name candidate-postgres -e POSTGRES_DB=candidatematch \
  -e POSTGRES_USER=candidatematch -e POSTGRES_PASSWORD=candidatematch123 \
  -p 5433:5432 -d postgres:15-alpine
```

- Or use docker-compose.yml (defaults map 5432:5432) and either:
    - Update application-local.yaml to point to 5432, or
    - Override at runtime: SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/candidatematch

Liquibase will create schemas/tables on startup. The pgvector extension and tables used for embeddings are gated by
the "pgvector" Liquibase context, which is enabled in the local profile.

## Quick start

- Run unit tests only (skip integration tests):

```bash
mvn -q -DskipITs=true -Dtest='*Test' verify
```

- Run integration tests (requires Docker and Testcontainers, including pgvector IT):

```bash
mvn -q -DskipITs=false -DrunPgVectorIT=true verify
```

- Start the app locally (uses the local profile and enables embeddings):

```bash
mvn -q spring-boot:run -Dspring-boot.run.profiles=local
```

## Embedding endpoints

Once the app is up (default http://localhost:8080), you can trigger embedding runs:

- Trigger Jason's embedding:

```bash
curl -X POST http://localhost:8080/api/embeddings/run/jason
```

- Trigger a specific user/cv:

```bash
curl -X POST "http://localhost:8080/api/embeddings/run?userId=thomas&cvId=andersen"
```

- Process missing embeddings in batches (default batchSize=50):

```bash
curl -X POST "http://localhost:8080/api/embeddings/run/missing?batchSize=100"
```

Requirements for real embeddings:

- embedding.enabled=true (in local profile)
- GEMINI_API_KEY set
- Embedding model defaults to text-embedding-004 and dimension 768

## Notes on tests and health checks

- Test naming patterns: unit tests "*Test"; integration tests "*IT" or "*IntegrationTest" (Failsafe).
- The pgvector integration test CvEmbeddingRepositoryIT is guarded by the system property runPgVectorIT=true to avoid
  failures where Docker isn't available.
- During test runs you may see health check error logs (database, Flowcase, GenAI). These are expected in isolated test
  contexts and do not indicate a failing build unless tests explicitly depend on those checks.

## macOS tips

- On Apple Silicon, Embedded Postgres used in some tests may use amd64 binaries via Rosetta. If needed, install Rosetta:

```bash
softwareupdate --install-rosetta --agree-to-license
```

## Troubleshooting

- Connection refused to Postgres on startup: ensure your container is running and the port matches your datasource URL (
  5433 by default in application-local.yaml).
- Empty embeddings returned: verify GEMINI_API_KEY is set and embedding.enabled=true.
- Flowcase stubs vs real API: tests stub Flowcase; for local runs ensure FLOWCASE_BASE_URL and FLOWCASE_API_KEY are
  configured for your environment.
