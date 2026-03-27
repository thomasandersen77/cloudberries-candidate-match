# Migrasjon fra Gemini til Ollama i Azure - FERDIGSTILT

## ✅ Hva er gjort

### 1. application-prod.yaml oppdatert
- ✅ `ai.provider: OLLAMA` (var GEMINI)
- ✅ `embedding.provider: OLLAMA` (var GEMINI)
- ✅ `projectrequest.analysis.provider: OLLAMA` (var GEMINI)
- ✅ Bruker `gemma3:12b-cv-expert-v4` modell
- ✅ Støtter `OLLAMA_BASE_URL` miljøvariabel
- ✅ Embedding-dimensjon endret til 1024 (for bge-m3)

### 2. GitHub Actions workflow oppdatert
- ✅ Validerer `OLLAMA_BASE_URL` secret
- ✅ Setter `OLLAMA_BASE_URL` som miljøvariabel i Container App
- ✅ Kommentar lagt til om at vi nå bruker Ollama for å unngå kostnader

## 🔧 Hva du må gjøre nå

### Steg 1: Finn Ollama Azure URL
Gå til Azure Portal og finn URLen til din Ollama Container App:

```bash
# Logg inn på Azure (hvis ikke allerede gjort)
az login

# Finn alle Container Apps i ressursgruppen
az containerapp list -g rg-candidate-match --query "[].{name:name, fqdn:properties.configuration.ingress.fqdn}" -o table
```

Du leter etter en container som heter noe som:
- `ollama-ca`
- `cloudberries-ollama`
- eller lignende

URLen vil se ut som: `https://ollama-ca.whitesand-767916af.westeurope.azurecontainerapps.io`

### Steg 2: Legg til GitHub Secret
1. Gå til GitHub repository: https://github.com/[din-org]/cloudberries-candidate-match
2. Gå til **Settings** → **Secrets and variables** → **Actions**
3. Klikk **New repository secret**
4. Navn: `OLLAMA_BASE_URL`
5. Verdi: URLen du fant i steg 1 (f.eks. `https://ollama-ca.whitesand-767916af.westeurope.azurecontainerapps.io`)
6. Klikk **Add secret**

### Steg 3: Verifiser Ollama-modeller
Sjekk at Ollama-instansen i Azure har disse modellene installert:
- `gemma3:12b-cv-expert-v4` (hovedmodell)
- `bge-m3` (embedding-modell)

Hvis ikke, må du laste dem opp til Ollama-instansen.

### Steg 4: Deploy til produksjon
Når `OLLAMA_BASE_URL` secret er satt, trigger en ny deployment:

```bash
# Push en endring til main branch, eller
git commit --allow-empty -m "Trigger deployment med Ollama-konfigurasjon"
git push origin main
```

Eller bruk GitHub Actions **Run workflow** knappen.

### Steg 5: Verifiser at det fungerer
Etter deployment, sjekk:

1. **Health check:**
   ```bash
   curl https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io/actuator/health
   ```

2. **Test CV scoring:**
   - Gå til frontend og prøv å score en CV
   - Scoren skal være lavere enn før (Gemma gir typisk 70-85 i stedet for 90-95 fra Gemini)
   - Oppsummeringen skal være kortere og mer direkte

3. **Sjekk logger:**
   ```bash
   az containerapp logs show -n cloudberries-candidate-match-ca -g rg-candidate-match --follow
   ```
   - Se etter meldinger om Ollama-tilkobling
   - Ingen feil om "GEMINI_API_KEY not found"

## 💰 Kostnadsbesparelse

### Før (med Gemini)
- Gemini API-kall: **~6000 NOK/måned** (basert på din tidligere regning)
- Per CV scoring: ~0.50-1.00 NOK
- Per chat-spørring: ~0.20-0.50 NOK

### Etter (med Ollama i Azure)
- Gemini API-kall: **0 NOK** ✅
- Ollama container runtime: ~200-400 NOK/måned (avhengig av VM-størrelse)
- **Total besparelse: ~5600 NOK/måned** 💰

## 🔒 Sikkerhet

### Fjern Gemini API-nøkkel (valgfritt)
Hvis du ikke trenger Gemini i det hele tatt, kan du fjerne API-nøkkelen:

1. Gå til GitHub repository → **Settings** → **Secrets and variables** → **Actions**
2. Slett `GEMINI_API_KEY` secret
3. Dette sikrer at ingen kan bruke Gemini ved et uhell

## ⚠️ Viktige notater

1. **Modellkvalitet:** Gemma 12B gir litt lavere kvalitet enn Gemini Pro, men er "god nok" for demo/produksjon
2. **Responstid:** Gemma kan være litt tregere enn Gemini (2-5 sekunder vs 1-2 sekunder)
3. **Skalering:** Hvis Ollama-containeren blir overbelastet, kan du øke VM-størrelsen i Azure

## 🐛 Feilsøking

### Problem: "Connection refused" til Ollama
**Løsning:** Sjekk at OLLAMA_BASE_URL er korrekt satt og at Ollama-containeren kjører:
```bash
az containerapp show -n ollama-ca -g rg-candidate-match --query "properties.runningStatus"
```

### Problem: "Model not found: gemma3:12b-cv-expert-v4"
**Løsning:** Last opp modellen til Ollama-instansen i Azure. Se `ollama-models/README-CV-TRAINING.md` for instruksjoner.

### Problem: Scoren er fortsatt høy (90-95) som Gemini
**Løsning:** Applikasjonen bruker fortsatt Gemini. Sjekk:
1. Er `OLLAMA_BASE_URL` riktig satt i GitHub Secrets?
2. Har du deployet etter å ha satt miljøvariabelen?
3. Sjekk Container App miljøvariabler i Azure Portal

### Problem: "OLLAMA_BASE_URL not found"
**Løsning:** Du har ikke satt GitHub Secret ennå. Se steg 2 ovenfor.

## 📊 Før/etter sammenligning

| Aspekt | Gemini (før) | Ollama (etter) |
|--------|--------------|----------------|
| Kostnad/måned | ~6000 NOK | ~300 NOK |
| CV Score (typisk) | 90-95 | 75-85 |
| Responstid | 1-2s | 2-5s |
| Oppsummering | Lang, detaljert, norsk | Kortere, direkte |
| API-avhengighet | Ja (Google) | Nei (self-hosted) |
| Data privacy | Google ser data | Kun i din Azure |

## ✅ Sjekkliste

- [ ] Funnet Ollama Azure URL
- [ ] Lagt til `OLLAMA_BASE_URL` i GitHub Secrets
- [ ] Verifisert at modellene finnes i Ollama
- [ ] Deployet til produksjon
- [ ] Testet CV scoring - score er lavere enn før
- [ ] Sjekket logger - ingen Gemini-feil
- [ ] (Valgfritt) Fjernet `GEMINI_API_KEY` secret

## 🎉 Når alt er ferdig

Gratulerer! Du har nå:
- ✅ Eliminert Gemini API-kostnader
- ✅ Byttet til self-hosted Ollama
- ✅ Beholder AI-funksjonalitet i produksjon
- ✅ Spart ~5600 NOK/måned

---

**Spørsmål?** Se `WARP.md` for mer informasjon om arkitekturen.
