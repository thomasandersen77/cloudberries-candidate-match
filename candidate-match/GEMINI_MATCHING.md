# Gemini 2.0 Flash Candidate Matching

## Oversikt

Ny matching-løsning som bruker **Gemini 2.0 Flash** med stor kontekstvindu til å rangere konsulenter mot kundeforespørsler.

### Hvordan det fungerer

1. **Kundeforespørsel lastes opp** (PDF eller tekst)
2. **Systemet henter relevante konsulenter** fra databasen basert på skills/kvalitet
3. **CV-tekst hentes fra database** (allerede lagret fra Flowcase-synk)
4. **Alle CV-er sendes inline til Gemini** i én API-request sammen med kundeforespørselen
5. **Gemini rangerer konsulentene** og returnerer strukturert JSON med score + begrunnelser
6. **Resultater lagres i database** for umiddelbar visning i frontend

## Arkitektur

### Clean Architecture / DDD

```
┌─────────────────────────────────────────┐
│        Application Layer                │
│  ┌───────────────────────────────────┐  │
│  │   GeminiMatchingPort (interface)  │  │ ← Dependency Inversion
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
                    ▲
                    │
┌─────────────────────────────────────────┐
│      Infrastructure Layer               │
│  ┌───────────────────────────────────┐  │
│  │  GeminiFileSearchAdapter          │  │ ← Implementation
│  │  (WebClient → Gemini API)         │  │
│  └───────────────────────────────────┘  │
│                                          │
│  ┌───────────────────────────────────┐  │
│  │  GeminiMatchingStrategy           │  │ ← Conditional Bean
│  │  (Builds CV text from database)   │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Dataflyt

```
ProjectRequest Upload
         ↓
   [Controller]
         ↓
ProjectMatchingServiceImpl
         ↓
   GeminiMatchingStrategy ────→ Fetch CVs from DB
         ↓                            ↓
         └──────────→ Format CV texts
                            ↓
                    GeminiFileSearchAdapter
                            ↓
                     [Gemini API Call]
                     • Project description
                     • All CVs inline (up to ~1M tokens)
                     • Request JSON response
                            ↓
                    JSON Response:
                    {
                      "ranked": [
                        {
                          "consultantId": "123",
                          "score": 85,
                          "reasons": ["...", "...", "..."]
                        }
                      ]
                    }
                            ↓
                   Store in Database
                            ↓
                    [Frontend displays]
```

## Konfigurasjon

### Environment Variables

```bash
# Required
export GEMINI_API_KEY=your-api-key-here

# Optional (has defaults)
export MATCHING_PROVIDER=GEMINI  # Activates Gemini matching
export MATCHING_TOP_N=10         # Number of candidates to return
```

### application.yaml

```yaml
gemini:
  apiKey: ${GEMINI_API_KEY}
  model: gemini-2.0-flash-exp  # Large 1M token context
  flashModel: gemini-2.0-flash-exp
  
matching:
  provider: GEMINI  # Switch between GEMINI / LEGACY
  topN: 10          # 5-15 recommended
  enabled: true
```

## API Endepunkter

### 1. Last opp kundeforespørsel (trigger matching)

```bash
POST /api/project-requests/upload
Content-Type: multipart/form-data

file: project-request.pdf
```

**Response**: `ProjectRequestResponseDto` + matching starter i bakgrunnen

### 2. Hent matching-resultater

```bash
GET /api/matches/{projectRequestId}?limit=10
```

**Response**:
```json
{
  "projectRequestId": 123,
  "projectTitle": "Senior Kotlin utvikler...",
  "totalMatches": 25,
  "matches": [
    {
      "consultantId": 456,
      "consultantName": "Thomas Andersen",
      "matchScore": 0.85,
      "matchExplanation": "[\"5+ års Kotlin erfaring\", \"Spring Boot ekspert\", \"Relevant bankerfaring\"]",
      "createdAt": "2025-11-22T15:00:00Z"
    }
  ],
  "lastUpdated": "2025-11-22T15:00:00Z"
}
```

### 3. Sjekk matching status

```bash
GET /api/matches/status/{projectRequestId}
```

## Implementeringsdetaljer

### CV-formattering

CV-er formateres strukturert for optimal AI-forståelse:

```
=== CV for Thomas Andersen ===

KOMPETANSE:
• Kotlin
• Spring Boot
• PostgreSQL

NØKKELKVALIFIKASJONER:
Senior utvikler:
  10+ års erfaring med backend-utvikling

PROSJEKTERFARING:
Kunde: DNB
  Roller: Tech Lead, Backend Developer
  Periode: 2020-01 - Pågående
  Beskrivelse: Modernisering av betalingsplatform
  Teknologier: Kotlin, Spring Boot, Kafka, PostgreSQL

[... mer erfaring ...]

UTDANNING:
• Master i Informatikk fra NTNU
```

### Prompt Engineering

**System Prompt**:
- Definerer rolle (teknisk rekrutteringsassistent)
- Spesifiserer vektlegging (må-krav > bør-krav)
- Krever strukturert JSON-respons

**User Prompt**:
- Kundeforespørsel (formatert med RequirementsExtractor)
- Alle CV-er inline (formatert tekst)
- Eksplisitt instruksjon om rangering

### Score-skala

- **0-100**: Gemini returnerer score i dette området
- **Konverteres til 0.0-1.0**: For kompatibilitet med eksisterende system
- **Reasons**: JSON array med konkrete begrunnelser

## Fordeler vs. gammel løsning

| Aspekt | Gammel (File Search) | Ny (Inline CVs) |
|--------|---------------------|-----------------|
| Setup | Kompleks (opplasting, store-management) | Enkel (ingen opplasting) |
| Latency | ~5-10s (fil-operasjoner) | ~2-4s (direkte API-kall) |
| CV-synk | Manuell trigger nødvendig | Automatisk (fra DB) |
| Kostnad | File Search + tokens | Kun tokens |
| Kontekst | Begrenset til store | Full 1M token kontekst |
| Debugging | Vanskelig (remote filer) | Lett (se prompt) |

## Testing

### Lokal testing

1. **Start database**:
```bash
docker-compose -f candidate-match/docker-compose-local.yaml up -d
```

2. **Sett API-nøkkel**:
```bash
export GEMINI_API_KEY=your-key
```

3. **Kjør applikasjon**:
```bash
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local
```

4. **Test med curl**:
```bash
# Last opp kundeforespørsel
curl -X POST http://localhost:8080/api/project-requests/upload \
  -F "file=@test-request.pdf"

# Hent resultater (vent 3-5 sek)
curl http://localhost:8080/api/matches/1
```

### Mock testing

For testing uten faktisk Gemini API-kall, sett:

```yaml
matching:
  provider: LEGACY  # Faller tilbake til gammel matching
```

## Feilhåndtering

### Graceful degradation

- **Gemini API nede**: Returnerer tom liste (logger error)
- **Invalid JSON fra Gemini**: Fallback til tom liste
- **Timeout**: 60s timeout, deretter retry eller tom liste
- **Rate limiting**: Exponential backoff (ikke implementert ennå)

### Logging

```kotlin
// Success
logger.info { "Successfully ranked 5 candidates" }

// Failure
logger.error(e) { "Failed to rank candidates for project 123" }
```

## Begrensninger

### Token limits

- **Input**: ~1M tokens (Gemini 2.0 Flash)
- **Estimat**: ~5000 tokens per CV
- **Max konsulenter**: ~200 CV-er per request (men vi filtrerer til 15-30)

### Performance

- **Responstid**: 2-5 sekunder per matching
- **Concurrent requests**: Begrenset av Gemini rate limits
- **Database queries**: O(n) for å hente CV-er

## Fremtidige forbedringer

1. **Caching**: Cache CV-formateringer per konsulent
2. **Batch processing**: Parallell matching av flere forespørsler
3. **A/B testing**: Sammenlign Gemini vs. legacy scoring
4. **Hybrid approach**: Kombinér strukturell filtrering + Gemini ranking
5. **Streaming**: Real-time progress updates til frontend

## Troubleshooting

### Problem: "Unresolved reference 'toDomain'"

**Fix**: Importer extension function:
```kotlin
import no.cloudberries.candidatematch.infrastructure.entities.toDomain
```

### Problem: "No matches returned"

**Check**:
1. Er `GEMINI_API_KEY` satt?
2. Er `matching.provider=GEMINI`?
3. Finnes det konsulenter med CV-er i databasen?
4. Sjekk logs for Gemini API errors

### Problem: "JSON parsing failed"

Gemini returnerte ugyldig JSON. **Fix**:
- Sjekk `responseMimeType: "application/json"` i config
- Se på raw response i logger
- Juster system prompt for tydeligere JSON-instruksjoner

## Kontakt

For spørsmål eller issues, kontakt utviklingsteamet eller opprett en issue i repoet.
