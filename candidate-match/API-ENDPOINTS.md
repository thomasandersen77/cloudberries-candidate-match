# Cloudberries Candidate Match API - Endepunkter

Dette dokumentet beskriver alle API-endepunkter i systemet på en funksjonell måte.

## Oversikt

| URL                                | Metode | Controller                     | Method                         | Query Parameters               | Request Body                             | Funksjonell Beskrivelse                                                                                                                                                                     |
|------------------------------------|--------|--------------------------------|--------------------------------|--------------------------------|------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/api/chatbot/analyze`             | POST   | AIController                   | analyzeContent                 | -                              | `{"content": "tekst"}`                   | Analyserer tekst med AI for å trekke ut innsikt eller strukturert informasjon. Brukes til å behandle CV-tekster eller prosjektbeskrivelser med kunstig intelligens.                         |
| `/api/consultants`                 | GET    | ConsultantController           | list                           | `name`, `page`, `size`, `sort` | -                                        | Henter en paginert liste over konsulenter fra Flowcase. Støtter navnefiltrering og sortering. Returnerer grunnleggende konsulentinformasjon som navn, e-post og fødselsår.                  |
| `/api/consultants/search`          | POST   | ConsultantController           | searchRelational               | -                              | `{ RelationalSearchRequest, pagination }` | Relasjonelt søk etter konsulenter. Paginering i body via `pagination` (page/size/sort). |
| `/api/consultants/search/semantic` | POST   | ConsultantController           | searchSemantic                 | -                              | `{ SemanticSearchRequest, pagination }`   | Semantisk søk (pgvector). Paginering i body via `pagination` (page/size/sort). |
| `/api/consultants/with-cv`         | GET    | ConsultantCvQueryController    | getAllWithCv                   | `onlyActiveCv`                 | -                                        | Henter alle konsulenter med normaliserte CV-data og strukturert informasjon om kvalifikasjoner, utdanning, arbeidserfaring og prosjekter.                                                   |
| `/api/consultants/sync/run`        | POST   | ConsultantSyncController       | syncAll                        | -                              | -                                        | Starter en manuell synkronisering av konsulentdata fra Flowcase til lokal database. Henter oppdaterte CV-er og konsulentinformasjon. Brukes når man vil sikre at lokale data er oppdaterte. |
| `/api/cv/{userId}`                 | GET    | CvController                   | getCv                          | -                              | -                                        | Henter komplett CV-data for en spesifikk konsulent direkte fra Flowcase. Returnerer rådata i JSON-format med all tilgjengelig CV-informasjon inkludert prosjekter og ferdigheter.           |
| `/api/embeddings/run/jason`        | POST   | EmbeddingController            | runJason                       | -                              | -                                        | Genererer vektor-embeddings for konsulenten "Jason" som en demo/test-funksjon. Brukes for å teste embedding-systemet og verifisere at AI-tjenesten fungerer korrekt.                        |
| `/api/embeddings/run`              | POST   | EmbeddingController            | runForUserCv                   | `userId`, `cvId`               | -                                        | Genererer vektor-embeddings for en spesifikk konsulents CV. Konverterer CV-tekst til numeriske vektorer som kan brukes for semantisk søk og sammenligning.                                  |
| `/api/embeddings/run/missing`      | POST   | EmbeddingController            | runMissing                     | `batchSize`                    | -                                        | Prosesserer alle konsulenter som mangler embeddings i batch. Sikrer at alle CV-er har tilhørende vektorer for semantisk søk. Kjører automatisk som bakgrunnsjobb.                           |
| `/api/health`                      | GET    | HealthController               | healthCheck                    | -                              | -                                        | Returnerer systemets helsestatus inkludert tilkobling til database, AI-tjenester og Flowcase. Brukes for overvåkning og feilsøking av systemkomponenter.                                    |
| `/api/matches`                     | POST   | MatchingController             | findMatches                    | -                              | `{"projectRequestText": "beskrivelse"}`  | Finner og rangerer konsulenter som matcher et prosjektkrav basert på AI-analyse. Sammenligner prosjektbeskrivelse med konsulent-CV-er og gir matchingpoeng med begrunnelse.                 |
| `/api/matches/upload`              | POST   | MatchingController             | findMatchesFromPdf             | -                              | Multipart: `file` + `projectRequestText` | Laster opp en CV i PDF-format og finner matchende prosjektmuligheter. Ekstraherer tekst fra PDF-en og sammenligner med eksisterende prosjektforespørsler i systemet.                        |
| `/api/cv-score/{candidateId}`      | GET    | CvScoreController              | getCvScoreForCandidate         | -                              | -                                        | Evaluerer og scorer en kandidats CV basert på kvalitet, komplettheter og relevans. Returnerer detaljert vurdering med styrker og forbedringsområder.                                        |
| `/api/cv-score/{candidateId}/run`  | POST   | CvScoreController              | runScoreForCandidate           | -                              | -                                        | Kjører CV-scoring for en spesifikk kandidat og returnerer detaljert vurdering med poeng og tilbakemelding.                                                                                  |
| `/api/cv-score/run/all`            | POST   | CvScoreController              | runScoreForAll                 | -                              | -                                        | Kjører CV-scoring for alle kandidater i systemet og returnerer sammendrag av prosesserte kandidater.                                                                                        |
| `/api/cv-score/all`                | GET    | CvScoreController              | getAllCandidates               | -                              | -                                        | Henter en oversikt over alle kandidater med deres grunnleggende informasjon. Gir en rask oversikt over konsulentdatabasen uten detaljerte CV-data.                                          |
| `/api/project-requests`            | GET    | ProjectRequestController       | listAll                        | -                              | -                                        | Henter alle lagrede kundeforespørsler med kompakt oversikt over tittel, kunde og krav.                                                                                                       |
| `/api/project-requests/upload`     | POST   | ProjectRequestController       | uploadAndAnalyze               | -                              | Multipart: `file`                        | Laster opp en kundeforespørsel (PDF), trekker ut krav via AI (må/bør), lagrer og returnerer resultatet.                                                                                     |
| `/api/project-requests/{id}`       | GET    | ProjectRequestController       | getById                        | -                              | -                                        | Henter en tidligere lagret kundeforespørsel med kravlistene (må/bør).                                                                                                                       |
| `/api/skills`                      | GET    | SkillsController               | listSkills                     | `skill` (repeater)             | -                                        | Henter aggregert oversikt over ferdigheter i selskapet, inkl. antall konsulenter og liste over konsulenter pr. ferdighet.                                                                   |

## Request Body Schemas

### RelationalSearchRequest (med paginering i body)

```json
{
  "name": "søketekst",
  "skillsAll": ["KOTLIN", "BACKEND"],
  "skillsAny": ["JAVA", "REACT"],
  "minQualityScore": 70,
  "onlyActiveCv": true,
  "pagination": { "page": 0, "size": 10, "sort": ["name,asc"] }
}
```

### SemanticSearchRequest (med paginering i body)

```json
{
  "text": "Senior Kotlin developer with Spring experience",
  "provider": "GOOGLE_GEMINI",
  "model": "text-embedding-004", 
  "topK": 5,
  "pagination": { "page": 0, "size": 10, "sort": ["name,asc"] }
}
```

## Ferdigheter (Skills)

Systemet støtter følgende ferdighetskategorier:

- `BACKEND` - Backend-utvikling
- `FRONTEND` - Frontend-utvikling
- `JAVA` - Java-programmering
- `KOTLIN` - Kotlin-programmering
- `REACT` - React-utvikling
- `TYPESCRIPT` - TypeScript-programmering
- `ARCHITECTURE` - Systemarkitektur

## Planlagte endepunkter (ikke implementert ennå)

Følgende endepunkter er planlagt men ikke implementert i gjeldende versjon:

| URL                                | Metode | Planlagt funksjonalitet                                              |
|------------------------------------|--------|----------------------------------------------------------------------|
| `/api/consultants/search`          | POST   | Relasjonelt søk etter konsulenter basert på strukturerte kriterier |
| `/api/consultants/search/semantic` | POST   | Semantisk søk ved hjelp av embeddings/pgvector                       |
| `/api/matches/by-skills`           | POST   | Finn matcher basert på valgte kompetanser                           |

## Notater

- Alle endepunkter returnerer JSON-data
- Paginering: GET-endepunkter bruker query-parametere (`page`, `size`, `sort`), mens POST-søk (relasjonelt/semantisk) bruker paginering i request-body via `pagination`.
- Semantisk søk krever at embeddings er generert for konsulentene
- AI-tjenester må være konfigurert og tilgjengelige for å fungere
