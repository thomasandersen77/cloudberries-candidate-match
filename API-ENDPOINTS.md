# Cloudberries Candidate Match API - Endepunkter

Dette dokumentet beskriver alle API-endepunkter i systemet på en funksjonell måte.

## Oversikt

| URL                                | Metode | Query Parameters               | Request Body                             | Funksjonell Beskrivelse                                                                                                                                                                     |
|------------------------------------|--------|--------------------------------|------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/api/chatbot/analyze`             | POST   | -                              | `{"content": "tekst"}`                   | Analyserer tekst med AI for å trekke ut innsikt eller strukturert informasjon. Brukes til å behandle CV-tekster eller prosjektbeskrivelser med kunstig intelligens.                         |
| `/api/consultants`                 | GET    | `name`, `page`, `size`, `sort` | -                                        | Henter en paginert liste over konsulenter fra Flowcase. Støtter navnefiltrering og sortering. Returnerer grunnleggende konsulentinformasjon som navn, e-post og fødselsår.                  |
| `/api/consultants/sync/run`        | POST   | `batchSize`                    | -                                        | Starter en manuell synkronisering av konsulentdata fra Flowcase til lokal database. Henter oppdaterte CV-er og konsulentinformasjon. Brukes når man vil sikre at lokale data er oppdaterte. |
| `/api/consultants/search`          | POST   | `page`, `size`, `sort`         | `RelationalSearchRequest`                | Søker etter konsulenter basert på strukturerte kriterier som navn, ferdigheter og CV-kvalitet. Bruker relasjonsdatabase-spørringer for å finne konsulenter som matcher spesifikke krav.     |
| `/api/consultants/search/semantic` | POST   | -                              | `SemanticSearchRequest`                  | Utfører semantisk søk ved å konvertere søketekst til vektor og sammenligne med CV-embeddings. Finner konsulenter basert på betydning og kontekst, ikke bare nøkkelord.                      |
| `/api/cv/{userId}`                 | GET    | -                              | -                                        | Henter komplett CV-data for en spesifikk konsulent direkte fra Flowcase. Returnerer rådata i JSON-format med all tilgjengelig CV-informasjon inkludert prosjekter og ferdigheter.           |
| `/api/embeddings/run/jason`        | POST   | -                              | -                                        | Genererer vektor-embeddings for konsulenten "Jason" som en demo/test-funksjon. Brukes for å teste embedding-systemet og verifisere at AI-tjenesten fungerer korrekt.                        |
| `/api/embeddings/run`              | POST   | `userId`, `cvId`               | -                                        | Genererer vektor-embeddings for en spesifikk konsulents CV. Konverterer CV-tekst til numeriske vektorer som kan brukes for semantisk søk og sammenligning.                                  |
| `/api/embeddings/run/missing`      | POST   | `batchSize`                    | -                                        | Prosesserer alle konsulenter som mangler embeddings i batch. Sikrer at alle CV-er har tilhørende vektorer for semantisk søk. Kjører automatisk som bakgrunnsjobb.                           |
| `/api/health`                      | GET    | -                              | -                                        | Returnerer systemets helsestatus inkludert tilkobling til database, AI-tjenester og Flowcase. Brukes for overvåkning og feilsøking av systemkomponenter.                                    |
| `/api/matches`                     | POST   | -                              | `{"projectRequestText": "beskrivelse"}`  | Finner og rangerer konsulenter som matcher et prosjektkrav basert på AI-analyse. Sammenligner prosjektbeskrivelse med konsulent-CV-er og gir matchingpoeng med begrunnelse.                 |
| `/api/matches/upload`              | POST   | -                              | Multipart: `file` + `projectRequestText` | Laster opp en CV i PDF-format og finner matchende prosjektmuligheter. Ekstraherer tekst fra PDF-en og sammenligner med eksisterende prosjektforespørsler i systemet.                        |
| `/api/cv-score/{candidateId}`      | GET    | -                              | -                                        | Evaluerer og scorer en kandidats CV basert på kvalitet, komplettheter og relevans. Returnerer detaljert vurdering med styrker og forbedringsområder.                                        |
| `/api/cv-score/all`                | GET    | -                              | -                                        | Henter en oversikt over alle kandidater med deres grunnleggende informasjon. Gir en rask oversikt over konsulentdatabasen uten detaljerte CV-data.                                          |
| `/api/project-requests/upload`     | POST   | -                              | Multipart: `file`                        | Laster opp en kundeforespørsel (PDF), trekker ut krav via AI (må/bør), lagrer og returnerer resultatet.                                                                                     |
| `/api/project-requests/{id}`       | GET    | -                              | -                                        | Henter en tidligere lagret kundeforespørsel med kravlistene (må/bør).                                                                                                                       |

## Request Body Schemas

### RelationalSearchRequest

```json
{
  "name": "søketekst",
  "skillsAll": ["KOTLIN", "BACKEND"],
  "skillsAny": ["JAVA", "REACT"],
  "minQualityScore": 70,
  "onlyActiveCv": true
}
```

### SemanticSearchRequest

```json
{
  "text": "Senior Kotlin developer with Spring experience",
  "provider": "GOOGLE_GEMINI",
  "model": "text-embedding-004", 
  "topK": 5
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

## Notater

- Alle endepunkter returnerer JSON-data
- Paginering følger standard Spring Boot-format med `page`, `size` og `sort` parametere
- Semantisk søk krever at embeddings er generert for konsulentene
- AI-tjenester må være konfigurert og tilgjengelige for å fungere