# Oppgave: Flytt all AI-kommunikasjon ut av Candidate Match og inn i AI RAG Service

Du jobber i et modulært monolitt-prosjekt med:

- `candidate-match` = core business logic
- `ai-platform-contracts` = Anti-Corruption Layer
- `ai-rag-service` = AI integrasjon / prompts / provider adapters

## Mål

Flytt **all kommunikasjon mot kunstig intelligens** ut av `candidate-match` og inn i `ai-rag-service`, uten å flytte business logic ut av core.

## Arkitekturkrav

### Core (`candidate-match`) skal eie
- business logic
- matching/scoring-regler
- repositories
- JPA entities
- transaksjoner
- use-case-orchestrering
- API-kontrakter som hører til core

### AI-modulen (`ai-rag-service`) skal eie
- Gemini-klient
- OpenAI-klient
- Ollama-klient
- Anthropic-klient
- embedding-adaptere
- prompt templates
- prompt rendering / prompt builders
- provider-valg / AI-factory
- AI-relatert parsing av provider-respons
- Spring AI / RAG-kode

### ACL (`ai-platform-contracts`) skal eie
- porter
- AI DTO-er
- kontraktstyper
- ingen Spring beans
- ingen repos
- ingen provider-kode

---

## Konkret oppgave

### 1. Identifiser all AI-spesifikk kode i `candidate-match`

Gå gjennom minst disse områdene:

- `infrastructure/integration/gemini/*`
- `infrastructure/integration/openai/*`
- `infrastructure/integration/ollama/*`
- `infrastructure/integration/anthropic/*`
- `infrastructure/integration/embedding/*`
- `infrastructure/integration/ai/AIContentGeneratorFactory.kt`
- `templates/*`
- `service/ai/*`

Vurder for hver fil:
- flyttes til `ai-rag-service`
- blir værende i core
- splittes i core-port + AI-adapter

### 2. Flytt provider-spesifikk kode til `ai-rag-service`

Flytt eller refaktorer:
- Gemini
- OpenAI
- Ollama
- Anthropic
- embedding providers
- prompt templates

slik at de ender i `ai-rag-service`.

### 3. La `candidate-match` bare bruke porter

Core-tjenester i `candidate-match` skal bruke porter fra `ai-platform-contracts`, ikke konkrete AI-klasser.

Eksempler:
- `ProjectRequestAnalysisService`
- `CandidateMatchingService`
- query interpretation / semantic search orchestration
- CV-analyse / AI-basert vurdering

Disse skal avhenge på porter, ikke på Gemini/Ollama/OpenAI/Anthropic-klasser.

### 4. Ikke flytt business logic ut av core

Dette skal bli i `candidate-match`:
- match score-logikk
- domenevalidering
- scoring-regler
- repository-kall
- transaksjoner
- controller-flow
- brukstilfelle-orchestrering

### 5. Flytt hardkodede prompts til `ai-rag-service`

Flytt alt av:
- inline prompts i services
- template-filer
- prompt rendering-funksjoner

inn i `ai-rag-service`.

Promptene skal ikke lenger ligge i core-modulen.

### 6. Rydd opp i duplicated contracts

Hvis `candidate-match` har egne porter eller AI-typer som dupliserer `ai-platform-contracts`, gjør følgende:

- behold én canonical versjon i `ai-platform-contracts`
- refaktorer `candidate-match` til å bruke contracts-modulen
- refaktorer `ai-rag-service` til å implementere contracts-portene

### 7. Behold dependency-retningen ren

Målet er denne dependency-retningen:

- `candidate-match` -> `ai-platform-contracts`
- `candidate-match` -> `ai-rag-service`
- `ai-rag-service` -> `ai-platform-contracts`

Ikke innfør:
- `ai-rag-service` -> `candidate-match`
- sirkulær dependency
- duplisering av core entities eller repositories

---

## Leveranseformat

Svar med:

1. What I looked at & why
2. Findings
3. Refactoring plan
4. Design sketch
5. Patch-style diffs
6. Liste over flyttede filer
7. Liste over porter som nå brukes fra `ai-platform-contracts`
8. Bygge-/wiring-endringer i POM og Spring-konfigurasjon

## Viktig kvalitetskrav

- ikke gjør kontroller tykkere
- ikke lek AI-provider-modeller inn i core domain
- ikke kopier repositories til AI-modulen
- ikke flytt transaksjoner til AI-modulen
- bevar dagens funksjonelle oppførsel så langt det er mulig