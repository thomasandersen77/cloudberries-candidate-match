# Feature: ProjectRequest med AI-analyse

## Oversikt
Denne featuren implementerer komplett funksjonalitet for håndtering av prosjektforespørsler med integrert AI-analyse for automatisk matching av konsulenter mot prosjektkrav.

## Kjernekomponenter

### Domain Layer
- `ProjectRequest`: Hoveddomene-entitet med validering av datoer og forretningsregler
- `AISuggestion`: Domene-modell for AI-genererte konsulent-anbefalinger
- `ProjectRequestId` og `CustomerId`: Verdi-objekter for type-sikkerhet

### Service Layer
- `ProjectRequestService`: Hovedservice for CRUD-operasjoner og forretningslogikk
- `AIService`: Integrerer med AI-tjenester for konsulent-matching og analyse
- `ConsultantWithCvService`: Håndterer søk og matching av konsulenter

### Infrastructure Layer
- `ProjectRequestRepository`: Database-tilgang for prosjektforespørsler
- `ProjectRequestEntity` og `AISuggestionEntity`: JPA-entiteter med korrekt mapping

### API Layer
- `ProjectRequestController`: REST-endepunkter for alle operasjoner
- DTO-mapping mellom API og domain-lag

## AI-analyse Funksjonalitet

### Automatisk Konsulent-matching
- Trigger ved opprettelse av ny prosjektforespørsel
- Analyserer påkrevde ferdigheter mot konsulent-profiler
- Genererer scorede anbefalinger med begrunnelser
- Fallback-mekanisme ved AI-tjeneste-feil

### Prompt Template System
AI-analysen bruker strukturerte prompt-templates for:
- Konsulent-evaluering mot prosjektkrav
- Scoring basert på erfaring, ferdigheter og tilgjengelighet
- Generering av menneskelig-forståelige begrunnelser

### Konfigurering
```yaml
ai:
  enabled: true
  timeout: 30s
  max-suggestions: 10
  fallback-enabled: true
```

## Validering og Forretningsregler

### Domeneregler
1. **Datovaldering**: Startdato må være før sluttdato
2. **Responsdeadline**: Svarfrist må være før prosjektstart
3. **Status-overgang**: Kun åpne forespørsler kan lukkes
4. **Påkrevde felt**: Kundenavn, beskrivelse og ansvarlig selger må angis

### Input-validering
- Sanitering av alle tekstfelt før AI-analyse
- Validering av e-postadresser og datoformater
- Begrensning av tekstlengde for å unngå prompt injection

## API-endepunkter

### Prosjektforespørsler
- `GET /api/project-requests` - Liste alle forespørsler (med paginering)
- `POST /api/project-requests` - Opprett ny forespørsel
- `POST /api/project-requests/upload` - Batch-upload via fil (multipart)
- `GET /api/project-requests/{id}` - Hent spesifikk forespørsel
- `PUT /api/project-requests/{id}/close` - Lukk forespørsel

### AI-analyse
- `POST /api/project-requests/{id}/analyze` - Trigger ny AI-analyse
- `GET /api/project-requests/{id}/suggestions` - Hent AI-anbefalinger

## Sikkerhet og Compliance

### Data Protection
- Ingen hardkoding av AI API-nøkler (bruker Spring Boot secrets)
- Logging uten sensitive data (maskerering av personnummer/CVer)
- Parametriserte SQL-spørringer for alle database-operasjoner

### Input Sanitization
- Validering og rensing før AI-prompts
- Begrensning av input-lengde
- Escape-sekvenser for SQL injection-beskyttelse

## Performance og Monitoring

### Database-optimalisering
- `@Timed`-annotasjoner på kritiske database-operasjoner
- Lazy loading for relasjoner
- Indeksering på søkefelt (kundenavn, ferdigheter, datoer)

### AI-tjeneste Monitoring
- `@Timed`-annotasjoner på AI-kall
- Circuit breaker-mønster for robusthet
- Fallback til regelbasert matching

### Logging og Observability
- Request-ID tracking i MDC (Mapped Diagnostic Context)
- Fargekodet konsoll-logging etter log-nivå:
  - ERROR: rød
  - WARN: orange (gul)
  - INFO: sort
  - DEBUG: grå
- Timing-informasjon via `@Timed` i loggene

## Testing Strategy

### Unit Tests
- Domene-logikk isolert fra infrastruktur
- Forretningsregler og valideringer
- Mocking av AI-tjenester
- Pattern: `*Test.kt` (Maven Surefire plugin)

### Integration Tests
- Database-interaksjoner med Testcontainers
- AI-tjeneste-integrasjon med WireMock
- End-to-end API-testing
- Pattern: `*IntegrationTest.kt` eller `*IT.kt` (Maven Failsafe plugin)

### Test Data
- Builder-pattern for testdata-generering
- Fixtures for komplekse scenarioer
- Mocking av eksterne tjenester

## Instruksjoner for Utvikling

### **GitHub Prompts Compliance**
**VIKTIG**: Ved all kode-utvikling på denne featuren skal instruksjonene under **GitHub Prompts** følges, med unntak av sikkerhetsvurderinger som håndteres separat:

1. **Architecture Review** (`architecture_review_prompt.txt`):
   - Følg DDD og lagdeling: controllers → services → domain → infra
   - Unngå sirkulære avhengigheter og lekkasje av infrastruktur
   - Sørg for klar ansvarsfordeling og DIP (interfaces)
   - Vurder ytelse: unngå N+1, bruk paginering og caching

2. **PR Review** (`pr_review_prompt.txt`):
   - Følg SOLID-prinsippene, spesielt SRP og DIP
   - Bruk meningsfulle navn som avslører intensjon
   - Skriv små funksjoner som gjør én ting
   - Unngå anemisk domene-modell - ha forretningslogikk i domain-objekter

3. **Readability Review** (`readability_review_prompt.txt`):
   - Navngivning som avslører intensjon
   - Små funksjoner med stegvis nedstigning
   - Fjerning av duplisering (DRY-prinsipp)
   - Konsistente mønstre og struktur

### Development Workflow
```bash
# Backend (Spring Boot + Kotlin)
cd /Users/tandersen/git/cloudberries-candidate-match

# Unit tests (fast - Surefire plugin)
mvn -q -DskipITs=true clean test

# Integration tests (Testcontainers - Failsafe plugin)  
mvn -q -DskipTests=true -DskipITs=false clean verify

# Build without tests
mvn -q -DskipTests=true -DskipITs=true clean package

# Run locally
mvn -q spring-boot:run -Dspring-boot.run.profiles=local
```

### Frontend Integration
```bash
# Synchronize OpenAPI schema
cp /Users/tandersen/git/cloudberries-candidate-match/openapi.yaml \
   /Users/tandersen/git/cloudberries-candidate-match-web/openapi.yaml

# Generate TypeScript types
npm --prefix /Users/tandersen/git/cloudberries-candidate-match-web run gen:api
```

### Code Quality Checklist
- [ ] Følger DDD-lagdeling og ansvarsfordeling
- [ ] Implementerer SOLID-prinsippene
- [ ] Har meningsfulle navn på klasser, metoder og variabler
- [ ] Små, fokuserte funksjoner med klar intensjon
- [ ] `@Timed`-annotasjoner på kritiske operasjoner
- [ ] Parametriserte SQL-spørringer
- [ ] Input-validering og -sanitering
- [ ] Comprehensive test coverage (unit + integration)
- [ ] Dokumenterte API-kontrakter i OpenAPI

## Fremtidige Utvidelser

### Planlagte Features
- Real-time notifikasjoner ved nye matches
- Machine learning-forbedring av matching-algoritmer
- Integrasjon med kalendersystemer for tilgjengelighetsvurdering
- Automatisk rapportgenerering og statistikk

### Teknisk Gjeld
- Refaktorering av legacy konsulent-søk til ny arkitektur
- Performance-optimalisering av embedding-beregninger
- Utvidelse av AI prompt templates for spesialiserte roller