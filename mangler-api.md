# Manglende API-endpoints eller data

Denne filen dokumenterer funksjonalitet vi har bygget i UI som forutsetter spesifikke API-endpoints eller datafelt.

## 1) Konsulentnavn i ConsultantDetailPage

- Vi leser navn fra `/api/cv/{userId}` og forsøker feltene `name` eller `fullName`.
- Dersom backend ikke returnerer ett av disse feltene, vises `userId` i stedet.
- For å sikre korrekt visning, anbefales et eksplisitt felt for visningsnavn i CV-responsen (for eksempel
  `displayName`).

## 2) Matches – kvalifikasjonssøk

- Vi støtter et “Kvalifikasjoner”-tab i UI der bruker oppgir ferdigheter.
- Vi gjenbruker `/api/matches` med `projectRequestText` der vi formaterer input som en tekstlig forespørsel, f.eks.:
    - `Finn konsulenter med ferdighetene: java, azure, react`.
- Dersom backend bør ha et mer strukturert endepunkt for kvalifikasjonssøk (for eksempel `/api/matches/by-skills` med
  `{ skills: string[] }`), er det “mangler” og kan spesifiseres.

## 3) Chat Analyze – modellvalg og metadata

- UI viser resultatfeltet `content` fra `/api/chatbot/analyze`.
- Om vi ønsker å vise hvilken modell som ble brukt (`modelUsed`) eller flere metadata, må backend returnere det
  konsekvent.

## 4) Health-detaljer

- UI viser alle nøkler/verdier under `details` som en tabell og fargekoder `UP`/`DOWN`.
- For bedre UX kan backend normalisere `details`-verdier til eksplisitt `UP`/`DOWN`-strenger.

## 5) CV-struktur

- UI rendrer hele CV-en som seksjoner/paragrafer fra rå JSON.
- For penere og mer konsistent presentasjon kan backend standardisere CV-strukturen (for eksempel feltene `summary`,
  `skills`, `experience`, `education`).
