Cloudberries Candidate Match - Modulær Prosjektstruktur

## Overordnet Arkitektur (The Modular Monolith)

```
cloudberries-candidate-match/                    (Root - Multi-modul Maven-prosjekt)
├── pom.xml                                      (Parent POM - orkestrator)
├── .env                                         (Miljøvariabler - API-nøkler)
├── .sdkmanrc                                    (Java 21.0.7-tem)
│
├── candidate-match/                             ⭐ PORT 8080 (Hovedmodul)
│   ├── src/main/kotlin/no/cloudberries/candidatematch/
│   │   ├── controllers/                         🌐 API Endepunkter
│   │   ├── service/                             ⚙️ Business Logic
│   │   ├── domain/                              🏛️ Domain Entities (Clean Code)
│   │   └── infrastructure/                      🔌 Adapters & External Integrations
│
├── ai-rag-service/                              🧠 PORT 8080 (Integrert RAG-modul)
│   ├── src/main/kotlin/no/cloudberries/ai/rag/  (RAG & Embeddings logic)
│
├── teknologi-barometer-service/                 📊 PORT 8082 (Teknologianalyse)
│
├── docs/                                        (Prosjektdokumentasjon)
└── [Diverse markdown-filer]                     (Utviklerdokumentasjon)
```

---

## Rot-nivå (`cloudberries-candidate-match-parent`)

### `pom.xml`
**Ansvar:** Parent POM som koordinerer moduler
- Definerer 2 submoduler: `candidate-match`, `teknologi-barometer-service`
- Minimal - inneholder kun modul-deklarasjoner
- Sentraliserer versjonshåndtering

### Viktige rot-filer
```
cloudberries-candidate-match/
├── pom.xml                                      # Parent POM
├── .env                                         # API-nøkler (GEMINI, OPENAI, FLOWCASE)
├── .sdkmanrc                                    # Java 21.0.7-tem (SDKMAN)
├── .gitignore                                   # Git-ekskluderinger
├── mvnw, mvnw.cmd                               # Maven Wrapper
│
├── WARP.md                                      # 📖 Hovedguide for WARP/utviklere
├── README.md                                    # Prosjektintroduksjon
├── openapi.yaml                                 # OpenAPI-spec (generert fra :8080)
├── API-ENDPOINTS.md                             # API-dokumentasjon
│
├── GEMINI_FILES_API_IMPLEMENTATION.md           # Gemini Files API-integrasjon
├── GEMINI_MODEL_GUIDE.md                        # Modellvalg-guide
├── CV_QUALITY_IMPLEMENTATION_GUIDE.md           # CV-kvalitetssystem
└── match_consultants.md                         # Matchingsalgoritmer
```

---

## Modul 1: `candidate-match/` ⭐

**Port:** 8080  
**Ansvar:** Hovedapplikasjon - konsulent-/prosjektmatching, CV-scoring, Flowcase-integrasjon, RAG & Embeddings

### Struktur
```
candidate-match/
├── pom.xml                                      # Spring Boot 3.3.x, Kotlin 2.2.0, Spring AI
├── docker-compose-local.yaml                    # PostgreSQL + pgvector (port 5433)
├── openapi.yaml                                 # REST API-spec
│
├── src/main/kotlin/no/cloudberries/candidatematch/
│   ├── Main.kt                                  # Spring Boot entry point
│   │
│   ├── rag/                                     # 🧠 RAG & Embeddings (Integrert)
│   │   ├── config/                              # Spring AI-konfigurasjon
│   │   ├── service/                             # RagService, Ingestion, Chunking
│   │   └── controller/                          # RAG API-endpoints (/api/rag/**)
│   │
│   ├── config/                                  # Konfigurasjon (DB, AI-klienter, CORS)
│   ├── health/                                  # Health checks (DB, Flowcase, Gemini)
│   │
│   ├── dto/                                     # Data Transfer Objects
│   │   ├── consultants/                         # Konsulent-DTOer
│   │   └── ai/                                  # AI-request/response DTOer
│   │
│   ├── matches/                                 # 🎯 Matching-domene
│   │   ├── domain/                              # Entiteter (ProjectRequest, MatchResult)
│   │   ├── repository/                          # JPA-repositories
│   │   ├── service/                             # Matchingslogikk (Gemini Files API)
│   │   ├── dto/                                 # Match-DTOer
│   │   └── event/                               # Domain events
│   │
│   ├── service/                                 # Business logic
│   │   ├── ConsultantService.kt                 # Konsulentdata fra Flowcase
│   │   ├── CvScoringService.kt                  # CV-kvalitetsanalyse
│   │   ├── EmbeddingService.kt                  # Embedding-generering (Gemini/OpenAI/Ollama)
│   │   └── GeminiFilesMatchingService.kt        # Gemini Files API matching
│   │
│   ├── application/ports/                       # Clean Architecture-porter
│   ├── templates/                               # Prompt-maler
│   └── utils/                                   # Hjelpefunksjoner
│
├── src/main/resources/
│   ├── application.yaml                         # Hovedkonfigurasjon
│   ├── application-local.yaml                   # Lokal profil (pgvector, embeddings, Ollama)
│   └── db/changelog/                            # Liquibase-migrasjoner
│
├── src/test/kotlin/                             # Tester (*Test.kt, *IT.kt)
│
└── docs/                                        # Modul-dokumentasjon
```

### Ansvarsområder
- **REST API:** Konsulenter, prosjekter, matching, CV-scoring, embeddings, RAG
- **Integrasjoner:** Flowcase, Gemini, OpenAI, Anthropic, Ollama (Spring AI)
- **Database:** PostgreSQL med pgvector-utvidelse (vektordata)
- **Matching:** Gemini Files API batch matching, semantisk søk
- **RAG:** Ingestion, chunking og kontekstualisert AI-generering
- **Swagger UI:** http://localhost:8080/swagger-ui/index.html

### Viktige Filer i candidate-match/
```
candidate-match/
├── docker-compose-local.yaml                    # Lokal PostgreSQL (port 5433)
├── docker-compose-test.yml                      # Test-database
├── docker-compose.yml                           # Produksjon
├── Dockerfile                                   # Container build
│
├── FEATURE_ENHANCED_PROJECT_MATCHING.md         # Feature-dokumentasjon
├── GEMINI_FILES_API_BATCH_MATCHING.md           # Batch matching-guide
├── GEMINI_MATCHING.md                           # Matching-strategi
├── GEMINI_MODEL_STRATEGY.md                     # Modellvalg
├── MODEL_SELECTION.md                           # Modellkonfigurasjon
│
├── README.md                                    # Modul-introduksjon
├── README-AZURE.md                              # Azure deployment
├── README-SDKMAN.md                             # SDKMAN-oppsett
│
├── flowcase.http                                # HTTP-tester (Flowcase)
├── health-checks.http                           # HTTP-tester (health)
└── test_ai_search.sh                            # Test-script
```

---

## Modul 2: `ai-rag-service/` 🧠

**Port:** 8081  
**Ansvar:** Retrieval-Augmented Generation, embedding-generering, semantisk søk

### Struktur
```
ai-rag-service/
├── pom.xml                                      # Spring AI 1.1.0-M2, pgvector
│
└── src/main/kotlin/no/cloudberries/airag/
    ├── config/                                  # Spring AI-konfigurasjon
    ├── service/                                 # Ingestion, chunking, embedding
    ├── controller/                              # RAG API-endpoints
    └── repository/                              # Vector store-tilgang
```

### Ansvarsområder
- **Chunking:** Splitter CV/jobbeskrivelser (400 tokens, 50 overlap)
- **Embeddings:** OpenAI `text-embedding-3-small` / Gemini `text-embedding-004`
- **Vector Store:** pgvector-integrasjon via Spring AI
- **Semantic Search:** Cosine similarity-søk
- **RAG Pipeline:** Hent relevant kontekst → LLM-generering

---

## Modul 3: `teknologi-barometer-service/` 📊

**Port:** 8082  
**Ansvar:** Teknologitrendanalyse fra jobbmarkedet (Gmail API)

### Struktur
```
teknologi-barometer-service/
├── pom.xml                                      # Gmail API, Apache Tika, Spring Boot
├── docker-compose-local.yaml                    # Egen PostgreSQL (port 5434)
├── README.md                                    # Setup-guide
│
└── src/main/kotlin/no/cloudberries/teknologibarometer/
    ├── config/                                  # Gmail API-konfigurasjon
    ├── service/
    │   ├── EmailIngestionService.kt             # Gmail-synkronisering
    │   ├── ContentExtractionService.kt          # Tika-parsing (PDF, HTML)
    │   └── TrendAnalysisService.kt              # AI-basert trendanalyse
    ├── domain/                                  # Entiteter (JobPosting, TechTrend)
    └── repository/                              # JPA-repositories
```

### Ansvarsområder
- **Gmail-integrasjon:** Henter jobbeskrivelser fra e-post
- **Content Extraction:** Apache Tika (PDF, HTML → tekst)
- **Trendanalyse:** OpenAI/Gemini for teknologi-/rolleanalyse
- **Metrics:** Prometheus-eksponering
- **Database:** Egen PostgreSQL (port 5434)

---

## Database-arkitektur

| Modul                        | Port  | Database           | Spesialiteter        |
|------------------------------|-------|--------------------|----------------------|
| `candidate-match`            | 5433  | `candidatematch`   | pgvector, RAG        |
| `teknologi-barometer-service`| 5434  | `teknologibarometer` | Egen DB              |

### Database-oppsett (candidate-match)
```yaml
# docker-compose-local.yaml
services:
  postgres-local:
    image: pgvector/pgvector:pg15
    container_name: cloudberries-postgres-local
    ports:
      - "5433:5432"
    environment:
      POSTGRES_DB: candidatematch
      POSTGRES_USER: candidatematch
      POSTGRES_PASSWORD: candidatematch123
```

---

## Ansvarsfordeling

### `candidate-match` ⭐ (The Magnificent Monolith)
- **Domain Owner:** Konsulenter, prosjekter, matching, CV-scoring, RAG
- **API Gateway:** REST API (port 8080)
- **Integrasjoner:** Flowcase, Gemini, OpenAI, Anthropic, Ollama
- **Teknologier:** Spring Boot, Spring AI, JPA, Liquibase, pgvector

### `teknologi-barometer-service` 📊 (Trendanalyse)
- **Domain Owner:** Jobbmarkedsdata, teknologitrender
- **Integrasjoner:** Gmail API, `candidate-match` (matching-logikk)
- **Teknologier:** Apache Tika, Prometheus, JPA

---

## Bygg- og kjørekommandoer

### Forutsetninger
```bash
# SDKMAN (required)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Java 21 Temurin
sdk install java 21.0.7-tem
sdk use java 21.0.7-tem

# Maven via SDKMAN
sdk install maven

# Docker Desktop (macOS)
brew install --cask docker
```

### Bygg alle moduler
```bash
# Fra prosjektrot
mvn -T 1C clean package

# Uten integrasjonstester (raskere)
mvn -T 1C -DskipITs=true clean package
```

### Kjør applikasjon (Port 8080)
```bash
# 1. Start database
cd candidate-match
docker-compose -f docker-compose-local.yaml up -d

# 2. Kjør applikasjon
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local
```

### Kjør teknologi-barometer-service
```bash
# Bruker egen database (port 5434)
mvn -pl teknologi-barometer-service -am spring-boot:run
```

### Testing
```bash
# Kun unit tests (raskt)
mvn -q -DskipITs=true clean test

# Kun integrasjonstester (krever Docker)
mvn -q -DskipTests=true -DskipITs=false clean verify

# Alle tester
mvn -q clean verify

# Module-spesifikk test
mvn -pl candidate-match -am test
```

---

## API-tilgangspunkter

### candidate-match (Port 8080)
- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **RAG API:** http://localhost:8080/api/rag/**
- **Health:** http://localhost:8080/actuator/health

### teknologi-barometer-service (Port 8082)
- **Health:** http://localhost:8082/actuator/health
- **Metrics:** http://localhost:8082/actuator/metrics
- **Prometheus:** http://localhost:8082/actuator/prometheus

---

## Miljøvariabler

### Rot-nivå (.env)
```bash
# Flowcase API
FLOWCASE_API_KEY=<your-key>
FLOWCASE_BASE_URL=https://cloudberries.flowcase.com/api

# OpenAI (teknologi-barometer, optional candidate-match)
OPENAI_API_KEY=<your-key>

# Gemini (candidate-match)
GEMINI_API_KEY=<your-key>

# Anthropic (candidate-match)
ANTHROPIC_API_KEY=<your-key>
```

### Database (candidate-match local profile)
```bash
POSTGRES_USER=candidatematch
POSTGRES_PASSWORD=candidatematch123
POSTGRES_DB=candidatematch
```

### Teknologi Barometer (Optional)
```bash
GMAIL_*=<gmail-api-credentials>
CANDIDATE_MATCH_URL=http://localhost:8080
```

---

## Teknologistabel

| Komponent           | Teknologi                          | Versjon      |
|---------------------|------------------------------------|--------------|
| **Java**            | Temurin JDK                        | 21.0.7-tem   |
| **Kotlin**          | Kotlin                             | 2.2.0        |
| **Build Tool**      | Maven                              | 3.x          |
| **Spring Boot**     | Spring Boot                        | 3.3.3/3.3.4  |
| **Database**        | PostgreSQL + pgvector              | 15           |
| **Migrations**      | Liquibase                          | -            |
| **Spring AI**       | Spring AI                          | 1.1.0-M2     |
| **AI APIs**         | Google Gemini, OpenAI              | -            |
| **HTTP Client**     | OkHttp                             | 5.1.0        |
| **PDF Parsing**     | Apache PDFBox                      | 2.0.30       |
| **Content Extract** | Apache Tika                        | 2.9.1        |
| **Gmail API**       | Google APIs Client                 | 2.2.0        |
| **Testing**         | JUnit 5, MockK, Testcontainers     | -            |
| **OpenAPI**         | Springdoc OpenAPI                  | 2.6.0        |

---

## Utviklingsflyt

### Daglig utvikling
```bash
# 1. Start lokal database
cd candidate-match
docker-compose -f docker-compose-local.yaml up -d

# 2. Bygg og kjør
mvn -T 1C clean package
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local

# 3. Utforsk API
open http://localhost:8080/swagger-ui/index.html

# 4. Kjør tester
mvn -q -DskipITs=true clean test
```

### OpenAPI-synkronisering (Frontend)
```bash
# Når candidate-match/openapi.yaml endres:
cp candidate-match/openapi.yaml ../cloudberries-candidate-match-web/openapi.yaml

# Eller generer fra kjørende service:
curl -s http://localhost:8080/v3/api-docs.yaml > ../cloudberries-candidate-match-web/openapi.yaml

# Regenerer frontend types (i frontend-repo):
cd ../cloudberries-candidate-match-web
npm run gen:api
```

---

## Vanlige Problemer

### Database-problemer
```bash
# Container starter ikke
docker-compose -f candidate-match/docker-compose-local.yaml logs postgres-local

# Port-konflikt
lsof -i :5433

# Test database-tilkobling
psql "host=localhost port=5433 dbname=candidatematch user=candidatematch password=candidatematch123"

# Verifiser pgvector
psql -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### API/Service-problemer
```bash
# Manglende API-nøkler
echo $GEMINI_API_KEY

# Health check
curl http://localhost:8080/actuator/health

# OpenAPI ikke oppdatert
cp candidate-match/openapi.yaml ../cloudberries-candidate-match-web/openapi.yaml
```

---

## Dokumentasjon

### Hovedfiler
- **WARP.md** - Komplett utviklerguide (mest oppdatert)
- **README.md** - Prosjekt-introduksjon
- **PROSJEKTSTRUKTUR.md** (denne filen) - Strukturoversikt

### Feature-dokumentasjon
- `GEMINI_FILES_API_IMPLEMENTATION.md` - Gemini Files API
- `CV_QUALITY_IMPLEMENTATION_GUIDE.md` - CV-kvalitetssystem
- `GEMINI_MODEL_GUIDE.md` - Modellvalg-strategier

### API-dokumentasjon
- `API-ENDPOINTS.md` - REST API-oversikt
- `openapi.yaml` - OpenAPI 3.0 spesifikasjon
- http://localhost:8080/swagger-ui/index.html - Interaktiv dokumentasjon

---

## Oppsummering

**Cloudberries Candidate Match** er et multi-modul Maven-prosjekt med to hovedkomponenter:

1. **candidate-match** ⭐ - Monolittisk applikasjon for konsulentmatching og RAG
2. **teknologi-barometer-service** 📊 - Jobbmarkedsanalyse

Hver modul har tydelig domenansvar, egen konfigurasjon, og (delvis) egen database. Prosjektet bruker moderne teknologi som Spring Boot 3, Kotlin 2.2, PostgreSQL med pgvector, og integrasjoner mot Gemini 3 Pro og OpenAI.

For detaljert utviklerinformasjon, se **WARP.md**.
