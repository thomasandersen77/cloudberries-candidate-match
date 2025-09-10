# Oppgave: Konsulent-domene – entiteter, jobb/PA-entiteter, CV og likviditetsreduksjon

Mål: Etabler en vedvarende datamodell (JPA) og tilhørende tjenester for konsulent-domenet, inkludert CV-struktur og en
beregning av likviditetsreduksjon som passer til disse modellene.

## Omfang og leveranser

1) Entiteter (JPA) for alle domeneklassene i konsultant-domenet

- For hver domene-klasse i konsultant-domenet, opprett en JPA-entitet under `infrastructure/entities/consultant`.
- Map alle felter, legg til annotasjoner for ID-generering, auditing-felter (createdAt/updatedAt) og nødvendig
  validering.
- Modellér relasjoner korrekt (OneToOne, OneToMany, ManyToOne, ManyToMany) med passende kaskader og fetch-typer.
- Håndter enum-typer og verdiobjekter på en konsistent måte.
- Benytt optimistisk låsing (@Version) der det er naturlig.

2) Jobb/PA-entiteter

- Opprett entiteter som representerer jobber/prosjektoppdrag (PA = Persistence Adapter/JPA-objekter i
  infrastruktur-laget) knyttet til konsulenter.
- Foreslåtte entiteter/felter: ProjectAssignment (tittel, start-/sluttdato, allokeringsgrad i %, timesats/kost,
  kunde/prosjekt-id, fakturerbarhet), EmploymentContract/Engagement ved behov.
- Relasjoner: Consultant 1..* ProjectAssignment; indekser på `consultant_id` og perioder for effektiv oppslag.

3) CV og relaterte under-objekter

- Modellér CV som egen entitet med støtte for flere versjoner per konsulent.
- Foreslåtte under-objekter: Experience, Education, Certification, Skill, Language, Attachment (PDF/fil-referanse).
- Relasjoner: Consultant 1..* CV; CV 1..* Experience/Education/Certification/Skill/Language.
- Legg på unike constraints der forretningsmessig riktig (f.eks. én aktiv hoved-CV per konsulent).

4) Avhengigheter og infrastruktur

- Sørg for nødvendige Maven-avhengigheter (spring-boot-starter-data-jpa, liquibase-core, postgresql-driver). Bruk Kotlin
  2.2/Java 21 og Spring Boot 3.3.x i tråd med prosjektets praksis.
- Opprett JPA-repositories for entitetene under `infrastructure/repositories/consultant`.
- Opprett adaptere/mapper-e mellom domenemodell og entiteter under `infrastructure/adapters`.
- Lag Liquibase-changelogs for alle tabeller, constraints, FK-er og indekser i `db/changelog`.


## Akseptansekriterier

- Alle entiteter persisteres og lastes korrekt via JPA; nødvendige relasjoner og constraints er på plass.
- Liquibase-migrasjoner kjører grønt ved `mvn verify` og under `docker-compose up`.
- Mapper/adapters dekker toveis mapping domenemodell ↔ entiteter, med representative tester.
- Kort dokumentasjon beskriver modellvalg og antakelser, spesielt rundt PA, CV-struktur og formel for
  likviditetsreduksjon.

## Tekniske rammer og retningslinjer

- Bruk Kotlin + Spring Data JPA; følg eksisterende pakkestruktur og navngiving i prosjektet.
- Optimaliser nøkler/indekser: primærnøkler (UUID anbefalt), unike constraints der det gir mening.
- Audit-felter: opprett og vedlikehold createdAt/updatedAt via JPA/Hibernate-mechanismer.
- Test: JUnit 5 + MockK; legg ved representative testdata.
- Database: PostgreSQL; for lokal utvikling brukes kun brukernavn/passord (ikke sertifikat-autentisering).

## Leveranseformat

- Pull request med kode, Liquibase-changelogs og tester.
- Kort notat i PR-beskrivelsen om datamodell og likviditetsreduksjonsformel/antakelser, samt eventuelle
  oppfølgingspunkter.

