# Warp Feature Instructions – Match Evaluering (Frontend + Backend)

## Formål
Utvikle en fullstendig “Match Evaluering”-feature i **Candidate Match**-systemet.  
Funksjonen skal identifisere hvilke konsulenter som passer best til en gitt prosjektforespørsel, og presentere resultatet som topp 10 med score og begrunnelse.  

---

## Overordnet funksjonalitet

1. Etter at en prosjektforespørsel lastes opp, skal backend starte en **jobb** som beregner hvilke konsulenter som matcher best.  
2. Matchingen skal kombinere:
   - Konsulentens **embedded CV-representasjon**  
   - En **generell CV-score** (kvalitetsindikator)  
   - Resultat fra **Gemini 2.5 Pro-modellen**, som brukes for å generere “matchScore” og en kort forklaring.  
3. Resultatet skal lagres i databasen, slik at historikk og siste beregning kan hentes effektivt.  
4. Frontend skal gi et visuelt grensesnitt som viser:
   - Alle prosjektforespørsler i tabellform  
   - Klikkbare rader som ekspanderer for å vise topp 10 kandidater  
   - Sortering etter **Match score** og **CV-score**  
   - En kort tekstforklaring per kandidat

---

## Lokasjoner og kontekst

### Backend-prosjekt
```
/Users/tandersen/git/cloudberries-candidate-match/candidate-match
```

Backend er et Spring Boot-prosjekt med JPA, Liquibase og eksisterende `MatchesController`.  
All ny kode og utvidelser skal legges i dette prosjektet.

**Mapper for ny kode:**
- `src/main/java/no/cloudberries/candidatematch/matches/`
  - `dto/` – dataobjekter som speiler JSON-strukturen i `MatchPromptTemplate`
  - `domain/` – JPA-entiteter og domeneobjekter for lagring av resultater
  - `repo/` – Spring Data repositories for matchresultater
  - `service/` – tjenestelag for beregning, uthenting og lagring
  - `web/` – REST-controller eller adapter til eksisterende `MatchesController`
  - `job/` – asynkron prosessering av matchberegninger

**Database og migrering:**
- Liquibase-filer skal legges under:
  ```
  src/main/resources/db/changelog/
  ```
- Databasen kjører lokalt på `localhost:5433`  
  Warp skal kunne kjøre migreringsscriptet automatisk mot denne.

**OpenAPI-synkronisering:**
- Backendens `openapi.yaml` ligger i rotmappen  
- Når nye endepunkt legges til, skal filen oppdateres og kopieres til frontend:
  ```
  /Users/tandersen/git/cloudberries-candidate-match-web/openapi.yaml
  ```

**Tester:**
- Opprett enhetstester for domene og service under:
  ```
  src/test/java/no/cloudberries/candidatematch/matches/
  ```
- Opprett integrasjonstester som bruker Testcontainers (PostgreSQL) for repo- og databasenivå.

---

### Frontend-prosjekt
```
/Users/tandersen/git/cloudberries-candidate-match-web
```

Frontend er et React + TypeScript-prosjekt.  
Siden `/matches` skal utvides og refaktoreres.

**Mapper for funksjonaliteten:**
- `src/types/` – opprett eller utvid typer for `ProjectRequestSummaryDto`, `MatchCandidateDto` og `MatchTop10Response`
- `src/api/` – legg til API-funksjoner som kaller:
  - `GET /api/matches/requests`
  - `POST /api/matches/requests/{id}/trigger`
  - `GET /api/matches/requests/{id}/top`
- `src/features/matches/` – refaktorer `/matches`-siden

**Frontend-oppsett for siden `/matches`:**
- Vis en tabell med alle prosjektforespørsler  
  - Kolonner: Tittel, Kunde, Opprettet-dato  
- Når en rad klikkes, skal den ekspandere og vise topp 10 kandidater  
  - Kolonner i den utvidede visningen: Navn, CV-score, Match-score, Begrunnelse  
  - Sortering: først på `matchScore`, deretter `cvScore`  
- Dersom ingen data finnes for en prosjektforespørsel, skal et API-kall trigge backend-jobben.  
  Når jobben er ferdig, skal UI automatisk hente og vise dataene.

---

## Backend-flyt (funksjonell beskrivelse)

1. **MatchesController**
   - Eksisterende controller skal utvides eller delegere til et nytt tjenestelag.
   - Tilbyr tre endepunkt:
     - `GET /api/matches/requests` → liste over alle prosjektforespørsler
     - `POST /api/matches/requests/{id}/trigger` → starter asynkron matchjobb
     - `GET /api/matches/requests/{id}/top` → returnerer siste lagrede topp 10-resultat

2. **MatchesService**
   - Skal håndtere domenelogikken:
     - Hente tilgjengelige prosjektforespørsler
     - Starte beregning av kandidater
     - Persistere resultater og begrunnelser
   - Bruk asynkron kjøring slik at brukeren kan fortsette mens jobben pågår.

3. **MatchEntity / MatchCandidateScoreEntity**
   - Representerer lagret resultat av en matchberegning.
   - Skal inneholde referanse til prosjektforespørsel, tidspunkt og tilhørende kandidater.

4. **ScoringGateway**
   - Modul som samler og rangerer kandidater basert på embedded CV-data og Gemini-score.
   - Kombinerer “matchScore” (fra modellen) og “cvScore” (fra CV-evaluering).

5. **Liquibase**
   - Opprett nye tabeller `match_result` og `match_candidate_score`.
   - Sørg for relasjon mellom resultat og kandidater (1:m).
   - Ingen hard foreign key mot prosjektforespørselstabell.

6. **Persistens**
   - Etter beregning skal topplisten lagres i databasen.
   - Ved gjentatt kall på samme prosjekt skal siste beregning hentes, ikke trigges på nytt med mindre bruker ber om det.

---

## Frontend-flyt (funksjonell beskrivelse)

1. **Ved lasting av /matches**
   - Kall `GET /api/matches/requests` for å hente alle prosjektforespørsler.
   - Vis dem i tabell.

2. **Når en rad ekspanderes**
   - Kall `GET /api/matches/requests/{id}/top`
   - Hvis tomt svar: kall `POST /api/matches/requests/{id}/trigger` og hent på nytt etter kort ventetid.

3. **Visning av topp 10**
   - Kandidater sorteres lokalt etter:
     1. Høyeste `matchScore`
     2. Høyeste `cvScore`
   - Vis “Begrunnelse” som kort tekstfelt fra modellen.

4. **Interaksjon**
   - Bruker kan ekspandere/skjule rader.
   - Bruker kan sortere om rekkefølgen (f.eks. mellom Match og CV-score).

---

## Test og validering

- Når backend og frontend er bygd:
  - Start backend med `./mvnw spring-boot:run`
  - Start frontend med `npm run dev`
- Gå til `http://localhost:5173/matches`
  - Verifiser at alle prosjektforespørsler vises
  - Klikk en rad → backend skal starte jobb og deretter vise topp 10 kandidater
  - Hver kandidat skal ha numeriske score og en forklaring
- Test at data persisteres og at reloading viser samme resultat uten re-kjøring.

---

## Leveranseforventning

Warp skal:
- Utføre hele feature-implementasjonen fra disse instruksjonene uten å stoppe.
- Gjenbruke eksisterende kode der det finnes.
- Ikke spørre om bekreftelse underveis.
- Lage alle nødvendige filer, mapper, klasser, scripts og typer.
- Kjøre backend-tester (unit + integration) og frontend-tester automatisk.
- Ikke pushe eller committe til Git — dette håndteres manuelt etter verifikasjon.
