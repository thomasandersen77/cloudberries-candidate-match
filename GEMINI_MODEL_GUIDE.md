# Gemini Model Guide - Velg Riktig Modell

## Oversikt

Applikasjonen bruker forskjellige Gemini-modeller for ulike oppgaver. Dette dokumentet forklarer hvordan du velger og bytter modeller enkelt.

## Tilgjengelige Modeller (November 2025)

### Gemini 3.0 Pro Preview 🌟
- **ID**: `gemini-3-pro-preview`
- **Kvalitet**: ⭐⭐⭐⭐⭐ (Smartest)
- **Hastighet**: 🐢 Sakte (~8-10s per request)
- **Bruk til**: Detaljert analyse av prosjektforespørsler
- **Kostnad**: Høyest (~$0.02 per 1K tokens)

### Gemini 2.5 Pro ✅ (ANBEFALT FOR MATCHING)
- **ID**: `gemini-2.5-pro`
- **Kvalitet**: ⭐⭐⭐⭐ (Veldig god)
- **Hastighet**: 🐇 Medium (~5-7s per request)
- **Bruk til**: Kandidat-ranking i batch
- **Kostnad**: Medium (~$0.01 per 1K tokens)
- **Hvorfor**: **Best balanse mellom kvalitet og hastighet**

### Gemini 2.5 Flash ⚡
- **ID**: `gemini-2.5-flash`
- **Kvalitet**: ⭐⭐⭐ (God)
- **Hastighet**: 🚀 Rask (~2-3s per request)
- **Bruk til**: Raske operasjoner, chat, quick queries
- **Kostnad**: Lavest (~$0.0005 per 1K tokens)

## Nåværende Konfigurasjon

### Prosjektanalyse (PDF Upload)
```yaml
gemini:
  model: gemini-3-pro-preview  # Beste kvalitet for Må/Bør-krav
```

**Hvorfor 3.0 Pro Preview?**
- Mest nøyaktig tolkning av komplekse krav
- Best til å skille "Må" vs "Bør"
- Forstår kontekst og implisitte krav
- Anbefalt fra Gemini-teamet

### Kandidat-Ranking (Batch Matching)
```yaml
matching:
  model: gemini-2.5-pro  # Anbefalt: Balanse kvalitet/hastighet
```

**Hvorfor 2.5 Pro?**
- ✅ Veldig god kvalitet (nesten like bra som 3.0)
- ✅ 40-50% raskere enn 3.0
- ✅ Lavere kostnad
- ✅ Stabil produksjonsmodell (ikke "preview")

## Hvordan Bytte Modell

### Metode 1: Edit Config Filer (Permanent)

**For Local Development** (`application-local.yaml`):

```yaml
# Prosjektanalyse
gemini:
  model: gemini-3-pro-preview  # Bytt til gemini-2.5-pro hvis 3.0 er for treg

# Matching  
matching:
  model: gemini-2.5-pro  # Alternativer:
                          # - gemini-3-pro-preview (best, tregere)
                          # - gemini-2.5-flash (raskere, litt dårligere)
```

**For Production** (`application.yaml`):
- Bruker environment variables (se Metode 2)

### Metode 2: Environment Variables (Anbefalt for Prod)

```bash
# Prosjektanalyse modell
export GEMINI_MODEL="gemini-3-pro-preview"

# Matching modell
export MATCHING_MODEL="gemini-2.5-pro"

# Flash modell (for raske operasjoner)
export GEMINI_FLASH_MODEL="gemini-2.5-flash"

# Restart application
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Metode 3: Runtime Properties (Temporary Testing)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local \
  -Dgemini.model=gemini-3-pro-preview \
  -Dmatching.model=gemini-2.5-flash
```

## Anbefalinger Per Use Case

### Scenario 1: Produksjon (Standard)
```yaml
gemini:
  model: gemini-3-pro-preview  # Beste analyse
  
matching:
  model: gemini-2.5-pro  # Balanse kvalitet/hastighet
```

**Forventet Performance**:
- PDF analyse: ~10s
- Kandidat-ranking (10 personer): ~6s
- Total matching: ~16s
- **Bra nok for async background jobs**

### Scenario 2: Utvikler-Testing (Rask Feedback)
```yaml
gemini:
  model: gemini-2.5-flash  # Rask testing
  
matching:
  model: gemini-2.5-flash  # Raskest mulig
```

**Forventet Performance**:
- PDF analyse: ~3s
- Kandidat-ranking: ~2s
- Total: ~5s
- **Perfekt for å teste flyt raskt**

### Scenario 3: Premium Kvalitet (Kostnad Ikke Viktig)
```yaml
gemini:
  model: gemini-3-pro-preview
  
matching:
  model: gemini-3-pro-preview  # Beste kvalitet
```

**Forventet Performance**:
- PDF analyse: ~10s
- Kandidat-ranking: ~9s
- Total: ~19s
- **Absolutt beste resultater, men tregt**

### Scenario 4: Kostnadsfokus (Budget-Optimalisert)
```yaml
gemini:
  model: gemini-2.5-pro  # God nok for de fleste
  
matching:
  model: gemini-2.5-flash  # 20x billigere enn Pro
```

**Forventet Performance**:
- PDF analyse: ~6s
- Kandidat-ranking: ~2s
- Total: ~8s
- **80% kvalitet til 25% kostnad**

## Kostnad Per Matching

| Konfigurasjon | Analyse | Matching | Total | Per Måned (100 matches) |
|---------------|---------|----------|-------|-------------------------|
| **Standard** (3.0 Pro + 2.5 Pro) | $0.20 | $0.10 | $0.30 | $30 |
| **Premium** (3.0 Pro + 3.0 Pro) | $0.20 | $0.20 | $0.40 | $40 |
| **Budget** (2.5 Pro + 2.5 Flash) | $0.10 | $0.01 | $0.11 | $11 |
| **Dev** (2.5 Flash + 2.5 Flash) | $0.005 | $0.005 | $0.01 | $1 |

*Estimater basert på 10 kandidater × 2K tokens per CV*

## Testing Av Modellbytte

### Test 1: Verifiser Ny Modell Fungerer

```bash
# Start app med ny modell
export MATCHING_MODEL="gemini-2.5-flash"
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Test matching endpoint
curl http://localhost:8080/api/matches/requests/30/top-consultants?limit=5

# Sjekk logs for modellnavn
grep "Using model:" logs/spring.log
```

### Test 2: Sammenlign Kvalitet

```bash
# Test med 2.5 Pro
export MATCHING_MODEL="gemini-2.5-pro"
curl http://localhost:8080/api/matches/requests/30/top-consultants > results-pro.json

# Test med 2.5 Flash
export MATCHING_MODEL="gemini-2.5-flash"
curl http://localhost:8080/api/matches/requests/30/top-consultants > results-flash.json

# Sammenlign results
diff results-pro.json results-flash.json
```

### Test 3: Måle Ytelse

```bash
# Med 2.5 Pro
time curl http://localhost:8080/api/matches/requests/30/top-consultants

# Med 2.5 Flash
time curl http://localhost:8080/api/matches/requests/30/top-consultants
```

## Feilsøking

### Problem: 404 Model Not Found

**Symptom**:
```
400 Bad Request: models/gemini-1.5-pro is not found for API version v1beta
```

**Løsning**:
```yaml
# FEIL - Gamle modeller som ikke finnes lenger:
model: gemini-1.5-pro
model: gemini-1.5-flash
model: gemini-2.0-flash-exp

# RIKTIG - Nye modeller (november 2025):
model: gemini-3-pro-preview
model: gemini-2.5-pro
model: gemini-2.5-flash
```

### Problem: For Treg Responstid

**Symptom**: Matching tar >15 sekunder

**Løsning**:
```yaml
# Bytt fra 3.0 Pro til 2.5 Pro for matching
matching:
  model: gemini-2.5-pro  # Eller gemini-2.5-flash hvis det haster
```

### Problem: Dårlig Match-Kvalitet

**Symptom**: Feil kandidater rangert høyest

**Løsning**:
```yaml
# Oppgrader til bedre modell
matching:
  model: gemini-3-pro-preview  # Beste kvalitet
```

## Best Practices

### ✅ DO

- ✅ Bruk **3.0 Pro Preview** for prosjektanalyse (kjøres sjelden, viktig)
- ✅ Bruk **2.5 Pro** for matching (god balanse)
- ✅ Bruk **2.5 Flash** for utvikler-testing
- ✅ Test nye modeller i dev først
- ✅ Monitorér kostnad vs kvalitet
- ✅ Bruk environment variables i prod

### ❌ DON'T

- ❌ Ikke bruk 3.0 Pro Preview for alt (for dyrt/tregt)
- ❌ Ikke bruk Flash for kritiske beslutninger
- ❌ Ikke hardkode modellnavn i kode
- ❌ Ikke glem å oppdatere begge configs (local + prod)
- ❌ Ikke bytt modell midt i en matching-jobb

## Fremtidige Modeller

Når nye Gemini-modeller blir tilgjengelige:

1. **Test i local først**:
   ```yaml
   # application-local.yaml
   model: gemini-x.y-new-model
   ```

2. **Verifiser kompatibilitet**:
   - Sjekk at API-endepunkt fungerer (`/v1beta/models/...`)
   - Test både upload og ranking
   - Sammenlign kvalitet med nåværende

3. **Oppdater dokumentasjon**:
   - Legg til i denne guiden
   - Oppdater kostnad-estimater
   - Oppdater anbefalinger

4. **Gradvis utrulling**:
   - Dev → Staging → 10% prod → 100% prod

## Referanser

- **Gemini Model List**: https://ai.google.dev/gemini-api/docs/models
- **Pricing**: https://ai.google.dev/pricing
- **Best Practices**: https://ai.google.dev/gemini-api/docs/thinking

---

**Sist oppdatert**: November 2025  
**Standard konfigurasjon**: 3.0 Pro Preview (analyse) + 2.5 Pro (matching)
