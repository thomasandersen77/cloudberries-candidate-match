# Teknologi Barometer Service

En mikroservice for analyse av teknologi-, rolle- og kompetanseetterspørsel i det norske tech-markedet basert på jobbhendelser fra portaler som Emagine, Verama og Knowit.

## Formål

Gi oversikt over hvilke **teknologier**, **profiler** og **kompetanseområder** som er mest etterspurt i markedet akkurat nå, basert på automatisk AI-analyse av jobbhendelser.

## Arkitektur

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Bronze Layer  │    │   Silver Layer   │    │   Gold Layer    │
│                 │    │                  │    │                 │
│ Raw Email Data  │───▶│ Structured Data  │───▶│ Trends & Insights│
│ • Gmail intake  │    │ • Technology     │    │ • Weekly trends │
│ • Attachments   │    │ • Profile        │    │ • Monthly growth│
│ • Deduplication │    │ • Competency     │    │ • Top demands   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### 3 AI-ekstraktorer for spesialisert analyse:

1. **Technology Extraction**: Identifiserer programmeringsspråk, rammeverk, databaser, cloud-plattformer
2. **Profile Extraction**: Analyserer rolletyper, senioritetssnivå, domeneområder, lokasjonstype
3. **Competency Extraction**: Kartlegger tekniske, domene-, metodiske og lederskap-kompetanser

## Database Schema (Bronze/Silver/Gold)

### Bronze Layer - Raw Data
- `bronze_email`: Rå e-post data fra Gmail API
- `bronze_attachment`: Vedlegg (PDF job descriptions)

### Silver Layer - Strukturerte Data  
- `silver_technology_demand`: Ekstrakterte teknologikrav med embeddings
- `silver_profile_demand`: Rolle/profil krav med kategorisering
- `silver_competency_demand`: Kompetansekrav etter område

### Gold Layer - Aggregerte Innsikter
- `gold_technology_trend`: Teknologitrends over tid
- `gold_profile_trend`: Rolle-etterspørsel trender  
- `gold_competency_trend`: Kompetanse-etterspørsel trender

## API Endepunkter

### Analyse-endepunkter
- `POST /api/barometer/analyze/technology` - Analyser teknologi-krav fra tekst
- `POST /api/barometer/analyze/profile` - Analyser rolle/profil krav fra tekst
- `POST /api/barometer/analyze/competency` - Analyser kompetanse-krav fra tekst
- `POST /api/barometer/analyze/all` - Kjør alle 3 analysene parallel

### Helsesjekk
- `GET /api/barometer/health` - Service helse-status

## Konfigurasjon

Tjenesten kjører på port **8082** (for å unngå konflikt med candidate-match på 8080).

### Database
- **Port**: 5434 (egen PostgreSQL database: `barometer_db`)
- **pgvector**: Aktivert for embeddings og semantisk søk

### Environment Variables
```bash
export POSTGRES_USER=barometer
export POSTGRES_PASSWORD=barometer123
export OPENAI_API_KEY=your_openai_key
export GMAIL_ENABLED=true
export GMAIL_USER=intake@cloudberries.no
```

## Kjøring

### Lokalt (utviklermode)
```bash
# Fra root av multi-modulprosjektet
mvn -pl teknologi-barometer-service spring-boot:run
```

### Med database
```bash
# Start PostgreSQL for barometer (port 5434)
docker run --name barometer-postgres \
  -e POSTGRES_DB=barometer_db \
  -e POSTGRES_USER=barometer \
  -e POSTGRES_PASSWORD=barometer123 \
  -p 5434:5432 -d postgres:15-alpine

# Kjør tjenesten
mvn -pl teknologi-barometer-service spring-boot:run
```

## Test API

```bash
# Test helsesjekk
curl http://localhost:8082/api/barometer/health

# Test teknologi-analyse
curl -X POST http://localhost:8082/api/barometer/analyze/technology \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Vi søker Senior Backend Developer med Kotlin og Spring Boot erfaring. PostgreSQL og AWS er viktig.",
    "portalSource": "Emagine"
  }'

# Test full analyse av jobbhendelse
curl -X POST http://localhost:8082/api/barometer/analyze/all \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Senior Full-stack Developer med 5+ års erfaring innen FinTech. Må kunne React, Node.js og PostgreSQL. Hybrid jobb i Oslo.",
    "portalSource": "Verama"
  }'
```

## Fremtidige Utvidelser

1. **Gmail Integrasjon**: Automatisk inntak fra `intake@cloudberries.no`
2. **Ekte AI-integrasjon**: Erstatt mock med OpenAI/Gemini API-kall  
3. **Trendanalyse**: Implementer Gold-layer aggregeringer
4. **UI Dashboard**: React frontend for visualisering (via ../cloudberries-candidate-match-web)
5. **Matching**: Integrasjon mot Candidate Match for å finne kandidater per opportunity

## Status: POC

Denne tjenesten er i POC-fase med mock AI-ekstraktorer. Databaseskjemaet og API-strukturen er klart for integrasjon med ekte AI-tjenester og Gmail-inntak.