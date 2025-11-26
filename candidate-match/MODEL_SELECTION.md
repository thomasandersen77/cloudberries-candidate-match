# Model Selection Guide: Gemini 2.0 Flash vs Gemini 3

## Quick Switch

### Method 1: Environment Variable (Anbefalt for prod)

```bash
# Gemini 2.0 Flash (standard - rask og billig)
export MATCHING_MODEL=gemini-2.0-flash-exp

# Gemini 3 Preview (premium - smart og nøyaktig)
export MATCHING_MODEL=gemini-3.0-pro-preview

# Restart applikasjon
mvn spring-boot:run
```

### Method 2: application-local.yaml (For lokal utvikling)

```yaml
matching:
  model: gemini-2.0-flash-exp        # Endre til gemini-3.0-pro-preview
```

## Sammenligningstabell

| Egenskap | Gemini 2.0 Flash | Gemini 3 Preview |
|----------|------------------|------------------|
| **Responstid** | ⚡ 2-4 sekunder | 🐌 8-15 sekunder |
| **Kostnad** | 💰 $0.10/1M tokens | 💸 ~$0.50/1M tokens (estimat) |
| **Nøyaktighet** | ✅ God (85-90%) | 🎯 Excellent (92-96%) |
| **Context** | 📊 1M tokens | 📊 1M tokens |
| **Stabilitet** | ✅ Production-ready | ⚠️ Preview (kan endre) |
| **Best for** | Volum, hastighet | Kvalitet, kompleksitet |

## Når bruke hvilken modell?

### ✅ Bruk Gemini 2.0 Flash når:

- Du behandler mange forespørsler per dag (>50)
- Kostnad er en viktig faktor
- Brukere forventer rask respons (<5 sek)
- Forespørslene er relativt enkle/standard
- Du er i utviklingsfase og tester raskt

**Eksempel scenario**: 
- Standard konsulent-matching for vanlige roller
- Høyt volum av søk per dag
- MVP/prototype-fase

### 🎯 Bruk Gemini 3 Preview når:

- Matching-kvalitet er kritisk
- Forespørslene er komplekse/nyanserte
- Du har budsjett for premium AI
- Volum er lavt-medium (<50 requests/dag)
- Strategiske/viktige kunder

**Eksempel scenario**:
- Lederroller eller spesialiserte stillinger
- VIP-kunder der nøyaktighet er viktigst
- Komplekse prosjekter med mange krav
- Når "feil match" er kostbart

## A/B Testing Setup

For å teste begge modeller parallelt:

### 1. Kjør to instanser

**Terminal 1 (Flash)**:
```bash
export MATCHING_MODEL=gemini-2.0-flash-exp
export SERVER_PORT=8080
mvn spring-boot:run
```

**Terminal 2 (Gemini 3)**:
```bash
export MATCHING_MODEL=gemini-3.0-pro-preview
export SERVER_PORT=8081
mvn spring-boot:run
```

### 2. Sammenlign resultater

```bash
# Test samme forespørsel mot begge
PROJECT_ID=123

# Flash
curl http://localhost:8080/api/matches/$PROJECT_ID > flash-results.json

# Gemini 3
curl http://localhost:8081/api/matches/$PROJECT_ID > gemini3-results.json

# Sammenlign
diff flash-results.json gemini3-results.json
```

### 3. Metrics å sammenligne

- **Responstid**: Mål tid fra request til response
- **Match score**: Er Gemini 3 sine scores høyere/bedre?
- **Reasons kvalitet**: Er begrunnelsene mer presise?
- **User feedback**: Be brukere rangere resultater

## Kostnad-estimat

### Gemini 2.0 Flash
```
Input: ~50,000 tokens per matching (10 CVs × 5000 tokens)
Kostnad: $0.10/1M tokens
Per matching: $0.005 (0.5 øre)
100 matchinger/dag: $0.50/dag = $15/måned
```

### Gemini 3 Preview
```
Input: ~50,000 tokens per matching
Kostnad: ~$0.50/1M tokens (estimat)
Per matching: $0.025 (2.5 øre)
100 matchinger/dag: $2.50/dag = $75/måned
```

**Differanse**: Gemini 3 er ~5x dyrere

## Performance Tuning

### Gemini 2.0 Flash optimalisering

```yaml
matching:
  model: gemini-2.0-flash-exp
  topN: 15  # Kan øke antall kandidater siden det er raskere
```

### Gemini 3 optimalisering

```yaml
matching:
  model: gemini-3.0-pro-preview
  topN: 10  # Hold lavere for å redusere responstid
```

## Hybrid Approach (Fremtidig forbedring)

Kombiner begge modeller:

1. **Første pass**: Bruk Flash til å filtrere ned til topp 20
2. **Andre pass**: Bruk Gemini 3 til å rangere de beste 20 → topp 5

Dette gir:
- ⚡ Rask filtrering (Flash)
- 🎯 Presis ranking (Gemini 3)
- 💰 Lavere kostnad (kun Gemini 3 på subset)

## Monitoring

Legg til logging for å spore modell-bruk:

```kotlin
// I GeminiFileSearchAdapter
logger.info { 
    "Using model: $model for project $projectRequestId " +
    "(candidates: ${candidates.size}, topN: $topN)"
}
```

### Metrics å tracke:

```
# Prometheus format
matching_requests_total{model="gemini-2.0-flash-exp"} 1250
matching_requests_total{model="gemini-3.0-pro-preview"} 80

matching_duration_seconds{model="gemini-2.0-flash-exp"} 3.2
matching_duration_seconds{model="gemini-3.0-pro-preview"} 12.5

matching_cost_usd{model="gemini-2.0-flash-exp"} 0.005
matching_cost_usd{model="gemini-3.0-pro-preview"} 0.025
```

## Decision Matrix

| Spørsmål | Ja → | Nei → |
|----------|------|-------|
| Er dette en strategisk/viktig kunde? | Gemini 3 | Flash |
| Er rollen senior/leder/spesialist? | Gemini 3 | Flash |
| Har vi budsjett til 5x kostnad? | Gemini 3 | Flash |
| Kan vi vente 10+ sekunder? | Gemini 3 | Flash |
| Er det >50 forespørsler/dag? | Flash | Gemini 3 |
| Er dette MVP/prototype? | Flash | Gemini 3 |

## Anbefaling

**Start med Gemini 2.0 Flash** for:
- Rask time-to-market
- Testing og validering av konseptet
- Volumbasert bruk
- Kostnadseffektivitet

**Upgrade til Gemini 3** når:
- Flash-resultater ikke er gode nok (bruker-feedback)
- Du har budsjett til premiumkvalitet
- Volum er lavt nok til at kostnad er akseptabel
- Nøyaktighet er business-critical

## Switching Checklist

Før du bytter modell:

- [ ] Test matching-kvalitet med begge modeller på samme dataset
- [ ] Beregn kostnad basert på forventet volum
- [ ] Verifiser at responstid er akseptabel for brukere
- [ ] Sjekk at API-nøkkel har tilgang til valgt modell
- [ ] Oppdater monitoring/alerting for ny kostnad/latency
- [ ] Kommuniser endring til stakeholders hvis relevant

## Support

For spørsmål om modellvalg, kontakt:
- Thomas Andersen (arkitekt)
- Dev team (implementering)
- Product owner (business requirements)
