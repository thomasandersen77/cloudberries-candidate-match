
# Oppgave: Stabiliser AI RAG Service før videre refaktor

Du jobber i et multi-modul Maven-prosjekt med disse modulene:

- `candidate-match` = core modul og eier av business logic, repositories, JPA entities og transaksjoner
- `ai-platform-contracts` = Anti-Corruption Layer / shared contracts
- `ai-rag-service` = AI-integrasjoner og RAG-modul

## Mål

Før du gjør videre refaktor, skal du stabilisere `ai-rag-service` slik at den kompilerer rent og følger riktig dependency-retning.

## Viktige regler

1. **Ingen sirkulær dependency**
    - `candidate-match` må ikke avhenge på konkrete AI-klasser
    - `ai-rag-service` må ikke eie core repositories eller core domain logic
    - `ai-platform-contracts` skal være ACL mellom core og AI-modul

2. **Core business logic skal bli i `candidate-match`**
    - ikke flytt matchingregler, scoringlogikk, repositories eller JPA entities ut av core

3. **All AI-kommunikasjon skal ende i `ai-rag-service`**
    - provider-klienter
    - prompt rendering
    - embedding-adaptere
    - Spring AI / RAG
    - providervalg / fallback

4. **`ai-platform-contracts` er source of truth for porter og AI-kontrakter**
    - bruk portene derfra
    - unngå parallelle port-definisjoner i andre moduler
    - unngå dupliserte AI-domain/DTO-klasser

---

## Det du skal gjøre nå

### 1. Analyser `ai-rag-service` for kompilasjonsproblemer

Gå gjennom:
- `ai-rag-service/pom.xml`
- `src/main/kotlin/no/cloudberries/ai/infrastructure/ai/*`
- `src/main/kotlin/no/cloudberries/ai/infrastructure/integration/*`
- `src/main/kotlin/no/cloudberries/ai/rag/*`
- `src/main/kotlin/no/cloudberries/ai/templates/*`

Identifiser:
- imports som peker til feil pakker
- typer som ikke finnes
- konflikter mellom egne typer og typer fra `ai-platform-contracts`
- Spring AI API-er som ikke matcher artifact-versjonen
- beans som mangler konfigurasjon
- gamle artifact-navn i POM som bør oppdateres

### 2. Normaliser Spring AI-avhengigheter

Se spesielt etter om disse artifactene er utdaterte eller feil:

- `spring-ai-ollama-spring-boot-starter`
- `spring-ai-pgvector-store-spring-boot-starter`

Foretrekk samme Spring AI-linje som resten av prosjektet bruker.

Mål:
- én konsistent Spring AI-versjon
- ett konsistent artifact-sett
- ingen miks av gammel og ny API-variant

### 3. Gjør `ai-rag-service` avhengig kun av:
- `ai-platform-contracts`
- Spring / Spring AI
- egne provider-klienter
- egne templates
- egne adaptere

Den skal **ikke** avhenge av:
- repositories i `candidate-match`
- JPA entities fra `candidate-match`
- core services fra `candidate-match`

### 4. Sørg for at adapterne implementerer kontrakt-portene

Adapterne i `ai-rag-service` skal implementere porter fra `ai-platform-contracts`, for eksempel:
- `AiContentGenerationPort`
- `CandidateMatchingPort`
- `ProjectRequestAnalysisPort`
- `QueryInterpretationPort`
- `EmbeddingPort`

Ikke redefiner disse portene lokalt dersom tilsvarende allerede finnes i contracts-modulen.

### 5. Rydd imports og typer

Hvis det finnes overlapp mellom:
- `no.cloudberries.ai.*`
- `no.cloudberries.candidatematch.*`

så skal AI-modulen bruke kontraktstypene fra `ai-platform-contracts`, ikke core-typer fra `candidate-match`.

### 6. Lever diff-ready endringer

Svar med:
1. What I looked at & why
2. Findings
3. Exact compile issues
4. Recommended fix
5. Patch-style diffs for:
    - `ai-rag-service/pom.xml`
    - relevante Kotlin-filer
6. Eventuelle testjusteringer

## Viktig

Ikke gjør stor funksjonell redesign i denne fasen.
Denne fasen handler kun om:
- kompilasjon
- dependency hygiene
- ren port/adapter-retning
- grunnlag for neste refaktor











---

# Summary

- Stabilized the `ai-rag-service` module to ensure it compiles cleanly and adheres to the "Modular Monolith" architectural constraints.
- Established `ai-platform-contracts` as the exclusive source of truth for AI-related ports and domain models, eliminating circular dependencies between the AI module and the core `candidate-match` module.

## Changes

- Updated `ai-rag-service/pom.xml` to include essential missing dependencies: `spring-boot-starter-webflux` (for WebClient), `jackson-module-kotlin`, `okhttp3`, and `kotlin-logging-jvm`.
- Normalized Spring AI versions in `ai-rag-service` to use the `${spring-ai.version}` property from the parent POM.
- Verified that all adapters in `ai-rag-service` (Ollama, Gemini, Anthropic, OpenAI) correctly implement the ports defined in `ai-platform-contracts`.
- Performed a codebase-wide audit of the AI module to ensure zero dependency on the `candidate-match` core module, strictly following the specified dependency direction.
- Resolved all unresolved reference errors and type inference issues that were preventing compilation.

## Verification

- Executed `./mvnw clean compile -pl ai-rag-service` and confirmed a successful build with no compilation errors.
- Verified the absence of `no.cloudberries.candidatematch.*` imports in the `ai-rag-service` module.
- Confirmed that all beans in the AI module are properly registered and follow the Port/Adapter pattern.