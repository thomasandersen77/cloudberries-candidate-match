# Cloudberries Candidate Match - Export Pakke

Dette er en komplett eksport av **cloudberries-candidate-match** prosjektet for bruk med ChatGPT.

## 📦 Innhold

### 1. **candidate-match-export.zip** (1.1 MB, 393 filer)
Komplett kildekode og konfigurasjon:
- ✅ Alle Kotlin source filer (`src/main/kotlin/`)
- ✅ Alle konfigurasjonsfilter (`application*.yaml`)
- ✅ Database migrasjoner (Liquibase changelogs)
- ✅ Test-filer og test resources
- ✅ `pom.xml` (Maven konfigurasjon)
- ✅ `openapi.yaml` (API spesifikasjon)
- ✅ **INSTRUKS_TIL_CHATGPT_GEMINI_FILES_API.md** (instruksjoner)

### 2. **INSTRUKS_TIL_CHATGPT_GEMINI_FILES_API.md**
Detaljert instruksjonsdokument med:
- 🎯 Rolle definisjon for ChatGPT
- 📋 Fullstendig kontekst om prosjektet
- 🏗️ Hybrid arkitektur tilnærming (anbefalt av Gemini)
- 📚 Gemini Files API referanse-dokumentasjon
- 🛠️ 4-stegs implementasjonsplan med full kode
- 🔧 Konfigurasjon eksempler
- 🧪 Test eksempler
- 📊 Kostnadsanalyse

## 🚀 Hvordan Bruke Dette Med ChatGPT

### Steg 1: Last Opp til ChatGPT
1. Gå til ChatGPT (chatgpt.com)
2. Start en ny samtale
3. Last opp **candidate-match-export.zip**

### Steg 2: Send Instruksjonen
Kopier hele innholdet fra **INSTRUKS_TIL_CHATGPT_GEMINI_FILES_API.md** og send til ChatGPT.

Alternativt, bruk denne korte prompten:

```
Jeg har lastet opp en zip-fil med et Spring Boot/Kotlin prosjekt.

Les filen INSTRUKS_TIL_CHATGPT_GEMINI_FILES_API.md for fullstendige instruksjoner.

Kort oppsummering:
- Jeg vil implementere Gemini Files API (Long Context) for CV-matching
- Bruk hybrid tilnærming: Database grovsortering → CV upload til Gemini → AI ranking
- Generer full kode for alle 4 steg (Liquibase, CV Converter, Gemini Adapter, Service)
- Bruk Spring WebClient og Kotlin coroutines
- Output format: Filnavn + komplett kode (klar for Warp terminal)

Start med å bekrefte at du har forstått prosjektstrukturen og arkitekturen.
```

### Steg 3: Implementer Koden
ChatGPT vil generere komplett kode for:
1. **Database migration** (Liquibase SQL)
2. **CvToMarkdownConverter.kt** (Service)
3. **GeminiFilesApiAdapter.kt** (Infrastructure)
4. **MatchesService.kt oppdateringer** (Application Service)

Kopier koden direkte fra ChatGPT til Warp terminal eller til filene i prosjektet.

## 📁 Prosjektstruktur

```
candidate-match/
├── src/
│   ├── main/
│   │   ├── kotlin/no/cloudberries/candidatematch/
│   │   │   ├── config/              # Konfigurasjon (GeminiProperties, WebClient)
│   │   │   ├── consultant/          # Konsulent domene
│   │   │   ├── cv/                  # CV domene
│   │   │   ├── infrastructure/      # Gemini adapters (eksisterende og nye)
│   │   │   ├── matches/             # Matching domene
│   │   │   ├── projectrequest/      # Prosjekt forespørsel domene
│   │   │   └── service/             # Service layer
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-local.yaml
│   │       ├── application-prod.yaml
│   │       └── db/changelog/        # Liquibase migrasjoner
│   └── test/
│       ├── kotlin/                  # Test filer
│       └── resources/               # Test resources
├── pom.xml
└── openapi.yaml
```

## 🎯 Mål med Implementasjonen

### Nåværende Tilstand
- ✅ Batch evaluering med Gemini API v1
- ✅ CVer sendes inline i request (opptil 20,000 tokens per request)
- ✅ 50-50 weighting (skills + CV quality)
- ✅ Topp 10 kandidater sendes til Gemini i ett kall

### Ønsket Tilstand (Files API)
- 🎯 CVer lastes opp til Gemini Files API som Markdown-filer
- 🎯 File URIs caches i database (`gemini_file_uri` kolonne)
- 🎯 Mindre request payload (file references istedenfor full CV tekst)
- 🎯 Bedre context caching (samme CVer brukes på tvers av requests)
- 🎯 Samme frontend respons-format (ingen breaking changes)

## 🔧 Nøkkel Komponenter Som Skal Implementeres

### 1. Database Migration
```sql
ALTER TABLE consultant_cv ADD COLUMN gemini_file_uri VARCHAR(512);
```

### 2. CvToMarkdownConverter
Konverterer CV-data til velstrukturert Markdown:
```markdown
# Konsulent Navn

## Informasjon
- ID: thomas.andersen
- E-post: thomas@cloudberries.no

## Ferdigheter
- **Kotlin** (5 år)
- **Spring Boot** (4 år)
...
```

### 3. GeminiFilesApiAdapter
Håndterer:
- Resumable file upload (2-stegs prosess)
- File URI caching
- Generate Content med `file_data` references

### 4. MatchesService Updates
Ny metode: `getTopConsultantsWithGeminiFilesApi()`
- Henter kandidater (SQL grovsortering)
- Scorer og velger topp 10
- Laster opp CVer (med caching)
- Kaller Gemini med file references
- Mapper til DTOs

## 📊 Forventet Resultat

### Logging (Example Output)
```
[MATCHING MODE] Using Gemini Files API (new approach with file uploads)
[STEP 1] Fetching candidate pool with 15 required skills
[STEP 1] Retrieved 39 consultants from database
[STEP 2] Scoring consultants by 50% skills + 50% CV quality
[STEP 2] Selected 10 consultants for Gemini evaluation
[STEP 2] Selected consultants: Thomas Andersen, Einar Flobak, ...
[STEP 3] Uploading CVs to Gemini Files API
[STEP 3] Prepared 10 file references
[STEP 4] Calling Gemini API with 10 file references in SINGLE request
[STEP 4] Gemini returned 5 ranked candidates
```

### API Response (Unchanged)
```json
[
  {
    "consultantId": 123,
    "userId": "thomas.andersen",
    "name": "Thomas Andersen",
    "matchScore": 92,
    "matchReasons": ["Extensive Kotlin experience", "Strong Spring Boot background"],
    "skills": ["Kotlin", "Spring Boot", "PostgreSQL"],
    "cvQuality": 85
  }
]
```

## 🔗 Viktige Referanser

- **Gemini Files API**: https://ai.google.dev/api/files
- **Long Context Guide**: https://ai.google.dev/gemini-api/docs/long-context
- **Spring WebClient**: https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client

## ⚠️ Viktige Notater

1. **Model Name**: Bruk `gemini-1.5-pro` eller `gemini-1.5-flash` (IKKE `gemini-3-pro-preview`)
2. **API Endpoint**: Files API bruker `/v1beta/` (ikke `/v1/`)
3. **MIME Type**: Bruk `text/markdown` for CVer
4. **File Expiry**: Gemini files utløper etter 48 timer (håndter re-upload)
5. **Caching**: Lagre `gemini_file_uri` i DB for å unngå unødvendige uploads
6. **Error Handling**: Graceful degradation - returner tom liste hvis Files API feiler

## 📞 Support

Hvis ChatGPT trenger mer kontekst:
- Vis til WARP.md fil (inkludert i zip)
- Forklar at prosjektet følger Clean Architecture/DDD
- Nevn at vi bruker Spring Boot 3.x, Kotlin, PostgreSQL med pgvector
- Repository pattern, Service layer, Infrastructure adapters

---

**Lykke til med implementasjonen! 🚀**

*Generert av Warp AI - 23. november 2025*
