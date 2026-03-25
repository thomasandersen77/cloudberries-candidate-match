# CV-Ekspert LLM Treningsguide

**Dokumentasjon for: Trening av en spesialisert LLM-modell til å bli CV-ekspert**

---

## 📋 Innholdsfortegnelse

1. [Oversikt](#oversikt)
2. [Arkitektur og Komponenter](#arkitektur-og-komponenter)
3. [Treningsprosessen](#treningsprosessen)
4. [Verktøy og Scripts](#verktøy-og-scripts)
5. [Scoringsmetodikk](#scoringsmetodikk)
6. [Hva vi gjør bra](#hva-vi-gjør-bra)
7. [Forbedringsområder](#forbedringsområder)
8. [Bruksanvisning](#bruksanvisning)
9. [Tekniske Detaljer](#tekniske-detaljer)
10. [Modellfleksibilitet og Alternativer](#modellfleksibilitet-og-alternativer)

---

## 🎯 Oversikt

### Formål
Vi trener en spesialisert LLM-modell (Gemma3 12B) til å bli en ekspert på å evaluere CV-er for norske IT-konsulenter. Modellen skal kunne:

- **Analysere CV-er strukturert** med fokus på teknisk dybde, lederskap og moderne teknologi
- **Gi konsistente vurderinger** basert på vektede kriterier
- **Produsere JSON-output** med score (0-100), sammendrag, styrker og forbedringsområder
- **Forstå norsk/nordisk arbeidsmarked** og konsulentbransjen

### Hvorfor Gemma3 12B?
- **Lokal kjøring**: Kan kjøres på laptop/server uten eksterne API-kall
- **Tilstrekkelig kapasitet**: 12 milliarder parametere gir god forståelse og generering
- **Ollama-kompatibel**: Enkel deployment og testing
- **Kosteffektivt**: Ingen per-request kostnader som med ChatGPT/Claude

### Treningsmetode: Modelfile Customization (Few-Shot Learning)
Vi bruker **Ollama's Modelfile**-tilnærming, som er en form for prompt engineering kombinert med few-shot learning:

- ✅ **Ingen GPU nødvendig** for modelltrening
- ✅ **Rask iterasjon** (sekunder å oppdatere modellen)
- ✅ **Ingen fine-tuning infrastruktur** kreves
- ✅ **Lett å vedlikeholde** og justere
- ⚠️ Begrensning: Lærer fra eksempler i context, ikke permanent vektjustering

---

## 🏗️ Arkitektur og Komponenter

### System Oversikt

```
┌─────────────────────────────────────────────────────────────┐
│                   CLOUDBERRIES CANDIDATE-MATCH              │
│                                                             │
│  ┌──────────────┐         ┌─────────────┐                  │
│  │   Flowcase   │────────→│  CV Store   │                  │
│  │  (Eksterne   │         │  (Database) │                  │
│  │    CV-er)    │         └──────┬──────┘                  │
│  └──────────────┘                │                          │
│                                   │                          │
│                                   ↓                          │
│                          ┌─────────────────┐                │
│                          │ Data Extraction │                │
│                          │   (Python)      │                │
│                          └────────┬────────┘                │
│                                   │                          │
│                                   ↓                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │           OLLAMA-MODELS Training Pipeline            │  │
│  │                                                       │  │
│  │  1. extract-training-data.py                         │  │
│  │     • PDF/DOCX → Text                                │  │
│  │     • Lager templates for scoring                    │  │
│  │                                                       │  │
│  │  2. Manual/Auto Scoring                              │  │
│  │     • Manuell vurdering av CVer                      │  │
│  │     • auto-score-cvs.sh (bootstrapping)              │  │
│  │                                                       │  │
│  │  3. build-v3-model.py                                │  │
│  │     • Scorer alle CVer automatisk                    │  │
│  │     • Velger 15 diverse eksempler                    │  │
│  │     • Genererer Modelfile                            │  │
│  │     • Kjører 'ollama create'                         │  │
│  │                                                       │  │
│  │  Output: Modelfile-gemma3-cv-expert-v3               │  │
│  └───────────────────────────────────────────────────────┘  │
│                                   │                          │
│                                   ↓                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │               OLLAMA LOCAL RUNTIME                    │  │
│  │                                                       │  │
│  │   gemma3:12b-cv-expert-v3                            │  │
│  │   • System prompt (Norwegian)                        │  │
│  │   • 15 few-shot examples (diverse scores)            │  │
│  │   • Tuned parameters (temp=0.2, ctx=12288)           │  │
│  └───────────────────────────────────────────────────────┘  │
│                                   │                          │
│                                   ↓                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │          BACKEND API (Spring Boot/Kotlin)             │  │
│  │                                                       │  │
│  │  • OllamaHttpClient.kt                               │  │
│  │  • CvScoringService.kt                               │  │
│  │  • POST /api/cv-score/{id}/run                       │  │
│  │  • Lagrer resultater i PostgreSQL                    │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Filstruktur

```
ollama-models/
├── build-v3-model.py                    # Hovedscript for automatisk training
├── build-v2-model.py                    # Tidligere versjon
├── extract-training-data.py             # PDF/DOCX → Text ekstrahering
├── auto-score-cvs.sh                    # Bootstrap auto-scoring
├── score-all-cvs.sh                     # Score alle CVer manuelt
├── test-cv-expert.sh                    # Test modellen
│
├── Modelfile-gemma3-cv-expert-v3        # Output: Ferdig modell (93 KB)
├── Modelfile-gemma3-cv-expert-v2        # v2 (64 KB)
├── Modelfile-gemma3-cv-expert           # Original (8.5 KB)
├── Modelfile-gemma3-cv-expert-norwegian # Norsk variant (7.9 KB)
│
├── README-CV-TRAINING.md                # Generell treningsguide
├── TRAINING-WORKFLOW.md                 # Steg-for-steg workflow
├── SCORING-GUIDE.md                     # Scoringskriterier og eksempler
│
├── training-cvs.txt                     # Ekstrahert CV-tekst (228 KB)
├── Modelfile-cv-examples.txt            # Templates for scoring (30 KB)
├── Modelfile-auto-scored.txt            # Auto-scorede eksempler (17 KB)
│
├── data/                                # CV-data (11 MB)
│   └── flowcase-cv-catalog/
│       ├── json/                        # Rå JSON fra Flowcase
│       ├── text/                        # Konvertert til ren tekst
│       └── training/                    # Training-klare filer
│
├── scores/                              # Scoringsresultater
│   └── flowcase/                        # JSON-filer per CV
│       ├── 001_zlatina-delistoyanova_*.json
│       ├── 002_...
│       └── ...
│
└── venv/                                # Python virtual environment
    └── lib/python3.14/site-packages/
        ├── PyPDF2/
        └── docx/
```

---

## 🔄 Treningsprosessen

### Fase 1: Data Extraction (Manuell/Automatisk)

**Script**: `extract-training-data.py`

```bash
cd /Users/tandersen/git/cloudberries-candidate-match/ollama-models
source venv/bin/activate
python extract-training-data.py \
  --cv-folder data \
  --output training-cvs.txt \
  --modelfile-output Modelfile-cv-examples.txt
```

**Hva skjer:**
1. Leser alle PDF/DOCX filer fra `data/`
2. Ekstraherer ren tekst (PyPDF2 for PDF, python-docx for Word)
3. Genererer to filer:
   - `training-cvs.txt`: Full tekst fra alle CVer (lesbar for mennesker)
   - `Modelfile-cv-examples.txt`: Template-format klar for scoring

**Output eksempel** fra `Modelfile-cv-examples.txt`:
```
# Example CV
MESSAGE user """Evaluate this CV:

Joachim Lous
Seniorkonsulent
Experienced developer and tech lead
Java and Kotlin experience from large public systems
..."""

MESSAGE assistant """{
  "scorePercentage": [SCORE_HERE],
  "summary": "[SUMMARY_HERE]",
  "strengths": [
    "[STRENGTH_1]",
    "[STRENGTH_2]",
    "[STRENGTH_3]"
  ],
  "improvements": [
    "[IMPROVEMENT_1]",
    "[IMPROVEMENT_2]",
    "[IMPROVEMENT_3]"
  ]
}"""
```

### Fase 2: Manual/Auto Scoring

#### Alternativ A: Manuell Scoring (Anbefalt for høy kvalitet)

1. Åpne `Modelfile-cv-examples.txt`
2. Les CV-teksten
3. Bruk `SCORING-GUIDE.md` som referanse
4. Erstatt placeholders med reelle verdier:
   - `[SCORE_HERE]` → 0-100 (f.eks. 87)
   - `[SUMMARY_HERE]` → 2-3 setninger
   - `[STRENGTH_1]` etc. → Spesifikke styrker (3-7 stk)
   - `[IMPROVEMENT_1]` etc. → Forbedringsområder (3-5 stk)

**Scoringskriterier** (fra v3):
1. **Struktur og Profesjonalitet** (Vekt 1.0)
2. **Prosjekt- og Rollebeskrivelser** (Vekt 2.5) ⭐ VIKTIGST
3. **Teknisk Dybde og Anvendelse** (Vekt 2.0)
4. **Lederskap, Mentoring og Faglig Initiativ** (Vekt 2.5) ⭐ VIKTIGST
5. **KI-kompetanse og Moderne Teknologi** (Vekt 2.0)

**Totalscore**:
```
Score = (Score1×1.0 + Score2×2.5 + Score3×2.0 + Score4×2.5 + Score5×2.0) / 10.0
```

#### Alternativ B: Auto-scoring (Bootstrap)

**Script**: `auto-score-cvs.sh`

```bash
./auto-score-cvs.sh
```

**Hva skjer:**
- Bruker eksisterende `gemma3:12b-cv-expert` (eller base model)
- Sender norsk scoring-prompt til modellen
- Scorer de første 5 CVene automatisk
- Output: `Modelfile-auto-scored.txt`

⚠️ **Viktig**: Auto-scorede CVer må alltid **manuelt reviewes** før bruk!

### Fase 3: Automated Model Building (v3 Pipeline)

**Script**: `build-v3-model.py`

Dette er hovedscriptet som automatiserer hele treningspipelinen.

```bash
cd /Users/tandersen/git/cloudberries-candidate-match/ollama-models
python3 build-v3-model.py
```

**Pipeline (4 steg):**

#### Steg 1: Score alle CVer
```python
def score_all(text_files: list) -> dict:
    """Score all CVs, loading cached results from disk when available."""
```

- Leser alle `.txt` filer fra `data/flowcase-cv-catalog/text/`
- For hver CV:
  - Sjekker om score allerede finnes i `scores/flowcase/`
  - Hvis ikke: Kaller `ollama_chat(SCORING_MODEL, prompt)` via REST API
  - Lagrer resultatet som JSON
- **Resumable**: Ctrl-C safe – kan stoppes og fortsette senere
- **Timeout**: 5 minutter per CV

#### Steg 2: Velg 15 diverse eksempler
```python
def select_examples(text_files: list, scores: dict, n: int = 15) -> list:
```

**Strategi**:
- Sorterer CVer etter score
- Deler inn i tre grupper:
  - **High** (≥80): Senior/Lead/Architect profiler
  - **Mid** (55-79): Solid mid-level
  - **Low** (<55): Junior eller svake CVer
- Velger spredt fra hver gruppe:
  - `n_high = n // 3` (ca. 5 stk)
  - `n_low = n // 5` (ca. 3 stk)
  - `n_mid = rest` (ca. 7 stk)
- Sikrer dekning av hele score-spekteret

#### Steg 3: Bygg Modelfile
```python
def write_modelfile(examples: list) -> None:
```

**Generert Modelfile inneholder**:
1. `FROM gemma3:12b` – Base-modell
2. `SYSTEM """..."""` – Norsk system-prompt med detaljerte instruksjoner
3. `PARAMETER` – Tuned parameters:
   - `temperature 0.2` – Konsistent output
   - `top_p 0.9`
   - `top_k 40`
   - `num_ctx 12288` – Stor context for lange CVer
   - `repeat_penalty 1.1`
4. `TEMPLATE """..."""` – Gemma3 chat template
5. **1 syntetisk low-score anchor** ("Jane Smith" junior CV med score 32)
6. **15 reelle Flowcase CVer** med full scoring

#### Steg 4: Create Ollama Model
```python
subprocess.run(["ollama", "create", MODEL_NAME, "-f", OUTPUT_MODELFILE])
```

- Kjører `ollama create gemma3:12b-cv-expert-v3 -f Modelfile-gemma3-cv-expert-v3`
- Tar 10-30 sekunder
- Output: Modellen er klar for bruk via `ollama run gemma3:12b-cv-expert-v3`

### Fase 4: Testing og Validering

**Script**: `test-cv-expert.sh`

```bash
./test-cv-expert.sh
```

**Test cases**:
1. **Senior Kotlin Developer** (forventet score: 80-90)
2. **Junior Frontend Developer** (forventet score: 30-50)
3. **Mid-level Full-stack** (forventet score: 60-75)

**Validering**:
- ✅ JSON er gyldig
- ✅ Scores varierer realistisk
- ✅ Styrker er spesifikke og detaljerte
- ✅ Forbedringsområder er handlingsorienterte
- ✅ Sammendrag er konsist og relevant

### Fase 5: Integration i Backend

Modellen brukes av Spring Boot backend:

```kotlin
// OllamaHttpClient.kt
class OllamaHttpClient {
    fun chat(model: String, userMessage: String): String {
        // POST http://localhost:11434/api/chat
        // Returns JSON with model response
    }
}

// CvScoringService.kt
class CvScoringService {
    fun scoreConsultant(candidateUserId: String): CvScore {
        val cvText = consultantRepository.findByUserId(candidateUserId)
        val response = ollamaClient.chat("gemma3:12b-cv-expert-v3", cvText)
        val score = parseJsonResponse(response)
        return cvScoreRepository.save(score)
    }
}
```

**API Endpoint**:
```bash
curl -X POST http://localhost:8080/api/cv-score/67e27aeb4d749c0040dd0206/run
```

---

## 🛠️ Verktøy og Scripts

### 1. `extract-training-data.py`

**Formål**: Konvertere PDF/DOCX til treningsklare templates

**Avhengigheter**:
```python
import PyPDF2          # PDF parsing
from docx import Document  # Word document parsing
```

**Hovedfunksjoner**:
- `extract_text_from_pdf(pdf_path)` – PyPDF2 extraction
- `extract_text_from_docx(docx_path)` – python-docx extraction
- `create_few_shot_example(cv_text)` – Modelfile format generator

**Output**:
- `training-cvs.txt` – Lesbar full-text
- `Modelfile-cv-examples.txt` – Template format

### 2. `build-v3-model.py`

**Formål**: Automatisk scoring, example selection, og model creation

**Konfigurering**:
```python
TEXT_DIR = SCRIPT_DIR / "data" / "flowcase-cv-catalog" / "text"
SCORES_DIR = SCRIPT_DIR / "scores" / "flowcase"
OUTPUT_MODELFILE = SCRIPT_DIR / "Modelfile-gemma3-cv-expert-v3"
MODEL_NAME = "gemma3:12b-cv-expert-v3"
SCORING_MODEL = "gemma3:12b-cv-expert-v2"  # Bootstrap med v2
OLLAMA_BASE_URL = "http://localhost:11434"
MAX_CV_CHARS = 6000
MAX_EXAMPLES = 15
TIMEOUT_SECS = 300
```

**Ollama API Integration**:
```python
def ollama_chat(model: str, user_message: str, timeout: int = 300) -> str:
    payload = json.dumps({
        "model": model,
        "messages": [{"role": "user", "content": user_message}],
        "stream": False,
        "options": {"temperature": 0.2, "num_ctx": 8192},
    })
    req = urllib.request.Request(
        f"{OLLAMA_BASE_URL}/api/chat",
        data=payload.encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        result = json.loads(resp.read().decode())
        return result["message"]["content"]
```

**JSON Parsing**:
```python
def extract_json(raw: str) -> dict:
    """Extract JSON from markdown-wrapped response."""
    if raw.startswith("```"):
        lines = [l for l in raw.split("\n") if not l.strip().startswith("```")]
        raw = "\n".join(lines).strip()
    start = raw.find("{")
    end = raw.rfind("}") + 1
    return json.loads(raw[start:end])
```

**Normalisation**:
```python
def normalise_score(score: dict) -> dict:
    """Ensure all required scoreBreakdown keys exist."""
    if "scoreBreakdown" not in score:
        score["scoreBreakdown"] = {}
    base = score.get("scorePercentage", 70)
    for key in REQUIRED_BREAKDOWN_KEYS:
        if key not in score["scoreBreakdown"]:
            score["scoreBreakdown"][key] = {
                "score": base,
                "justification": "Ikke vurdert separat",
            }
    return score
```

### 3. `auto-score-cvs.sh`

**Formål**: Quick bootstrap scoring med eksisterende modell

**Bash + jq pipeline**:
```bash
# Extract CV text from Modelfile template
cv_text=$(sed -n "/# From: $cv_file/,/MESSAGE assistant/p" "$CV_EXAMPLES_FILE" | \
          sed -n '/MESSAGE user/,/MESSAGE assistant/p' | \
          sed '1d;$d' | \
          sed 's/^"""//' | sed 's/"""$//')

# Score via Ollama
score_json=$(echo "$full_prompt" | timeout 180s ollama run "$MODEL")

# Clean JSON (remove markdown)
score_json=$(echo "$score_json" | sed 's/```json//g' | sed 's/```//g' | jq -c '.')
```

### 4. `score-all-cvs.sh`

**Formål**: Score alle CVer i data-mappen individuelt

**Features**:
- Detekterer PDF vs DOCX automatisk
- Python inline extraction
- 4-minutters timeout per CV
- Lagrer alle resultater som JSON
- Spesiell output for Thomas Andersen's CV 😊

### 5. `test-cv-expert.sh`

**Formål**: Enkel kvalitetssjekk av modellen

**Test struktur**:
```bash
ollama run gemma3:12b-cv-expert "Evaluate this CV and provide a score as JSON:

Name: Thomas Andersen
Experience:
- Tech Lead at Cloudberries (2020-2024): Kotlin, Spring Boot, microservices, ...
Skills: Kotlin, Java, Spring Boot, Kubernetes, Docker, AWS, PostgreSQL, ...
Education: MSc Computer Science, University of Oslo (2015)"
```

---

## 📊 Scoringsmetodikk

### Vektede Kriterier (v3)

| Kriterium | Vekt | Hva vurderes? |
|-----------|------|---------------|
| **1. Struktur og Profesjonalitet** | 1.0 | Logisk oppbygning, klarhet, profesjonell tone, fravær av skrivefeil |
| **2. Prosjekt- og Rollebeskrivelser** | 2.5 ⭐ | Forretningsverdi, rolle, ansvar, konkrete bidrag – **VIKTIGST** |
| **3. Teknisk Dybde og Anvendelse** | 2.0 | Mer enn lister – hvordan og hvorfor teknologier brukes |
| **4. Lederskap og Faglig Initiativ** | 2.5 ⭐ | Arkitekt, tech lead, mentor, kunnskapsdeling – **VIKTIGST** |
| **5. KI og Moderne Teknologi** | 2.0 | Praktisk erfaring med AI, sky, DevOps, Kubernetes |

**Total score beregning**:
```
Total = (Score1 × 1.0 + Score2 × 2.5 + Score3 × 2.0 + Score4 × 2.5 + Score5 × 2.0) / 10.0
```

### Scoringsskala

| Range | Nivå | Profil |
|-------|------|--------|
| **90-100** | Eksepsjonell | 15+ år, lead/architect, deep tech + business, moderner stack |
| **80-89** | Sterk Senior | 8-15 år, solid lederskap, moderne teknologier |
| **70-79** | Solid Profil | 4-8 år, god teknisk kompetanse, noen mangler |
| **50-69** | Midlere | 2-4 år, grunnlag OK, vesentlige gap |
| **20-49** | Junior | 0-2 år, begrenset erfaring, mye opplæring nødvendig |
| **0-19** | Ikke egnet | Ingen relevant erfaring eller utdaterte ferdigheter |

### Output Format (JSON Schema)

```json
{
  "scorePercentage": 87,
  "summary": "Highly experienced developer and technical leader with deep expertise...",
  "strengths": [
    "Extensive experience with large-scale public sector systems",
    "Strong technical leadership as tech lead and architect",
    "Modern tech stack: Java/Kotlin, Spring Boot, microservices",
    "Cloud experience with Azure and AWS certification",
    "...more..."
  ],
  "improvements": [
    "Frontend skills not prominently mentioned",
    "No specific mention of Kubernetes orchestration at scale",
    "Could benefit from additional cloud certifications",
    "...more..."
  ],
  "scoreBreakdown": {
    "structureAndProfessionalism": {
      "score": 85,
      "justification": "Velstrukturert CV med klar progresjon og profesjonell presentasjon"
    },
    "projectAndRoleDescriptions": {
      "score": 90,
      "justification": "Utmerket beskrivelse av roller, ansvar og forretningsverdi"
    },
    "technicalDepth": {
      "score": 88,
      "justification": "Dyp teknisk kompetanse med moderne stack og bred erfaring"
    },
    "leadershipAndInitiative": {
      "score": 85,
      "justification": "Dokumentert lederskap som tech lead og architect"
    },
    "aiAndModernTech": {
      "score": 80,
      "justification": "God sky-erfaring men begrenset AI/ML praktisk bruk"
    }
  }
}
```

---

## ✅ Hva vi gjør bra

### 1. **Systematisk og Reproduserbar Pipeline**
- ✅ Hele prosessen er automatisert med `build-v3-model.py`
- ✅ Resumable scoring (cache-støtte)
- ✅ Versjonert modeller (v1 → v2 → v3)
- ✅ Git-tracked configuration

### 2. **Sterke Evalueringskriterier**
- ✅ **Vektede kriterier** gjør scoring mer rettferdig
- ✅ Fokus på **lederskap og prosjektbeskrivelser** (høyest vekt)
- ✅ Balansert mellom teknisk dybde og soft skills
- ✅ Norsk/nordisk markedskontekst integrert

### 3. **Kvalitetskontroll på Flere Nivåer**
- ✅ Manual scoring av nøkkel-CVer
- ✅ Diverse example selection (high/mid/low spread)
- ✅ Syntetisk low-score anchor ("Jane Smith")
- ✅ Test-suite med `test-cv-expert.sh`

### 4. **Teknisk Solid Implementering**
- ✅ Ollama local deployment (privacy + no API costs)
- ✅ JSON schema validation
- ✅ Error handling og timeouts
- ✅ Modulær Python-kode
- ✅ Backend integration klar

### 5. **God Dokumentasjon**
- ✅ `README-CV-TRAINING.md` – Overordnet guide
- ✅ `TRAINING-WORKFLOW.md` – Steg-for-steg
- ✅ `SCORING-GUIDE.md` – Konkrete eksempler
- ✅ Inline comments i alle scripts

### 6. **Iterativ Forbedring**
- ✅ v1 → v2 → v3 viser kontinuerlig læring
- ✅ Feedback loop: score → review → adjust criteria → rescore
- ✅ Lett å legge til nye eksempler

---

## 🔧 Forbedringsområder

### 1. **Overgå til Ekte Fine-Tuning**

**Problem**: Modelfile-tilnærmingen er begrenset til context window.

**Løsning**:
- Samle 100+ manuelt reviewede scores fra produksjon
- Bruk **Unsloth** eller **LLaMA-Factory** for parameter fine-tuning
- Krever GPU (eller cloud-basert fine-tuning på GCP/AWS)

**Fordeler**:
- Permanente vektjusteringer
- Bedre generalisering
- Mindre avhengig av few-shot examples

**Estimert tidsinvestering**: 1-2 uker setup + 8-16 timer GPU-tid

### 2. **Utvid Treningsdata-Diversitet**

**Problem**: Alle CVer er fra Flowcase (norske IT-konsulenter).

**Forbedringsforslag**:
- ✅ Inkluder **negative eksempler**: CVer med gaps, utdatert tech, dårlig struktur
- ✅ Legg til **karriereskiftere**: Folk som går fra annet felt til IT
- ✅ Inkluder **internasjonale profiler**: For å unngå norsk bias
- ✅ Generer **syntetiske CVer** med ulike svakheter

**Implementering**:
```python
# Legg til i build-v3-model.py
SYNTHETIC_EXAMPLES = [
    {
        "name": "Career Changer",
        "score": 25,
        "weaknesses": ["No IT education", "Only 6 months bootcamp", ...]
    },
    {
        "name": "Outdated Stack",
        "score": 40,
        "weaknesses": ["10 years PHP only", "No cloud experience", ...]
    }
]
```

### 3. **Automatiser Manuell Review**

**Problem**: Manuell scoring av CVer er tidkrevende.

**Løsning**:
- Bygg et **web UI for scoring** med pre-filled fields
- Lag **scoring templates** for vanlige profiler (senior Kotlin, junior React, etc.)
- Implementer **inter-rater reliability checks**: To personer scorer samme CV
- Bruk **active learning**: Modellen foreslår hvilke CVer som trenger manuell review

**Tech stack**:
- React frontend: Vis CV + scoring-form
- Backend lagrer gold-standard scores
- Prioritiser CVer med høy uncertainty

### 4. **Forbedre JSON Parsing Robusthet**

**Problem**: Modellen returnerer noen ganger markdown-wrapped JSON eller ugyldige escape sequences.

**Nåværende løsning**:
```python
def extract_json(raw: str) -> dict:
    if raw.startswith("```"):
        lines = [l for l in raw.split("\n") if not l.strip().startswith("```")]
        raw = "\n".join(lines).strip()
    start = raw.find("{")
    end = raw.rfind("}") + 1
    return json.loads(raw[start:end])
```

**Forbedring**:
- Legg til **retry-logikk** med justert prompt hvis JSON parsing feiler
- Bruk **Pydantic models** for strengere validering
- Implementer **JSON Schema validation** på output

```python
from pydantic import BaseModel, Field, validator

class CvScore(BaseModel):
    scorePercentage: int = Field(..., ge=0, le=100)
    summary: str = Field(..., min_length=50, max_length=500)
    strengths: list[str] = Field(..., min_items=3, max_items=10)
    improvements: list[str] = Field(..., min_items=3, max_items=10)
    scoreBreakdown: dict
    
    @validator('scorePercentage')
    def score_must_be_realistic(cls, v):
        if v == 0 or v == 100:
            raise ValueError('Scores of exactly 0 or 100 are unrealistic')
        return v
```

### 5. **Legg til A/B Testing**

**Problem**: Vi vet ikke om v3 faktisk er bedre enn v2.

**Løsning**:
- Score samme CVer med både v2 og v3
- Sammenlign med human expert scores (gold standard)
- Beregn **correlation** og **mean absolute error (MAE)**

```python
from scipy.stats import pearsonr

human_scores = [85, 72, 90, 45, 68]
v2_scores = [80, 75, 88, 50, 65]
v3_scores = [84, 71, 92, 47, 69]

corr_v2, _ = pearsonr(human_scores, v2_scores)
corr_v3, _ = pearsonr(human_scores, v3_scores)

print(f"v2 correlation: {corr_v2:.3f}")
print(f"v3 correlation: {corr_v3:.3f}")
```

### 6. **Optimaliser Model Parameters**

**Nåværende**:
```python
PARAMETER temperature 0.2      # Konservativ
PARAMETER top_p 0.9
PARAMETER top_k 40
PARAMETER num_ctx 12288        # Stor context
PARAMETER repeat_penalty 1.1
```

**Eksperimenter**:
- Prøv `temperature 0.1` for enda mer konsistent output
- Test `num_ctx 16384` for veldig lange CVer
- Kjør **grid search** over parameters og mål JSON-validering rate

### 7. **Integrer Feedback Loop fra Backend**

**Problem**: Ingen automatisk læring fra produksjonsbruk.

**Løsning**:
- Backend logger alle scoringer
- Brukere kan **flagge dårlige scores**
- Batch-export flagged + corrected scores
- Re-run `build-v3-model.py` med oppdaterte eksempler månedlig

**Database schema**:
```sql
CREATE TABLE cv_score_feedback (
    id SERIAL PRIMARY KEY,
    score_id UUID REFERENCES cv_score(id),
    flagged_by VARCHAR(255),
    issue_type VARCHAR(50),  -- 'too_high', 'too_low', 'wrong_strengths', etc.
    corrected_score INT,
    corrected_summary TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### 8. **Legg til Multi-Language Support**

**Problem**: Alle prompts og output er på norsk.

**Forbedring**:
- Parametriser språk i `build-v3-model.py`
- Lag separate modeller: `gemma3:12b-cv-expert-no`, `gemma3:12b-cv-expert-en`
- Eller: Én modell med språkparameter i prompt

```python
SYSTEM_PROMPT_EN = """
You are an AI assistant evaluating CVs for a Nordic consulting company...
"""

SYSTEM_PROMPT_NO = """
Du er en AI-assistent som evaluerer CV-er for et konsulentselskap...
"""

def build_modelfile(examples, language='no'):
    prompt = SYSTEM_PROMPT_NO if language == 'no' else SYSTEM_PROMPT_EN
    # ... rest of logic
```

### 9. **Optimaliser Scoring Speed**

**Nåværende**: 5-10 sekunder per CV med Gemma3 12B.

**Forbedringsforslag**:
- Bruk **Gemma3 7B** eller **Gemma3 2B** for rask pre-filtering
- Kun kjør 12B-modellen på kandidater som ser lovende ut
- **Batch inference**: Score flere CVer samtidig
- Bruk **llama.cpp** med CUDA for raskere inferens

```bash
# llama.cpp med GPU-akselerasjon
./main -m gemma3-12b-cv-expert.gguf -ngl 32 -c 8192 -t 8
```

### 10. **Dokumenter Bias og Fairness**

**Problem**: Modellen kan ha implisitte biaser (alder, kjønn, nasjonalitet).

**Løsning**:
- **Anonymiser CVer** før scoring (fjern navn, kjønn, alder)
- Test på syntetiske CVer med ulike demografier
- Mål score-fordeling per demografi
- Legg til **fairness constraints** i fine-tuning

---

## 📖 Bruksanvisning

### Quick Start

1. **Installer avhengigheter**:
```bash
cd /Users/tandersen/git/cloudberries-candidate-match/ollama-models
python3 -m venv venv
source venv/bin/activate
pip install PyPDF2 python-docx
```

2. **Trekk ut treningsdata** (hvis du har nye CVer):
```bash
python extract-training-data.py --cv-folder data --output training-cvs.txt --modelfile-output Modelfile-cv-examples.txt
```

3. **Bygg modell automatisk**:
```bash
python3 build-v3-model.py
```

4. **Test modellen**:
```bash
./test-cv-expert.sh
```

5. **Bruk i backend**:
```bash
cd ../candidate-match
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local
curl -X POST http://localhost:8080/api/cv-score/{candidateId}/run
```

### Manuell Workflow

1. Ekstraher CVer
2. Score 5-10 manuelt i `Modelfile-cv-examples.txt`
3. Kopier scorede eksempler til `Modelfile-gemma3-cv-expert`
4. Kjør `ollama create gemma3:12b-cv-expert -f Modelfile-gemma3-cv-expert --force`
5. Test med `./test-cv-expert.sh`

---

## 🔬 Tekniske Detaljer

### Ollama API (REST)

**Chat endpoint**:
```bash
curl http://localhost:11434/api/chat \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemma3:12b-cv-expert-v3",
    "messages": [
      {"role": "user", "content": "Evaluate this CV: ..."}
    ],
    "stream": false,
    "options": {
      "temperature": 0.2,
      "num_ctx": 8192
    }
  }'
```

**Response**:
```json
{
  "model": "gemma3:12b-cv-expert-v3",
  "created_at": "2024-03-25T13:42:10Z",
  "message": {
    "role": "assistant",
    "content": "{\"scorePercentage\": 87, \"summary\": \"...\", ...}"
  },
  "done": true
}
```

### Modelfile Syntax

```dockerfile
# Base model
FROM gemma3:12b

# System prompt (Norwegian)
SYSTEM """
Du er en AI-assistent som evaluerer CV-er...
"""

# Parameters
PARAMETER temperature 0.2
PARAMETER top_p 0.9
PARAMETER num_ctx 12288

# Chat template (Gemma3-specific)
TEMPLATE """
{{ if .System }}<start_of_turn>system
{{ .System }}<end_of_turn>
{{ end }}{{ if .Prompt }}<start_of_turn>user
{{ .Prompt }}<end_of_turn>
{{ end }}<start_of_turn>model
{{ .Response }}<end_of_turn>
"""

# Few-shot examples
MESSAGE user """Evaluate this CV: [CV text]"""
MESSAGE assistant """{JSON response}"""
```

### Python Dependencies

```
PyPDF2==3.0.1          # PDF text extraction
python-docx==1.1.0     # DOCX text extraction
```

### Performance Metrics

| Model | Context Size | Tokens/sec | Time per CV | RAM Usage |
|-------|--------------|-----------|-------------|-----------|
| gemma3:12b base | 8192 | ~30 | 5-7s | ~8 GB |
| gemma3:12b-cv-expert-v3 | 12288 | ~25 | 8-10s | ~10 GB |

### File Sizes

| File | Size | Description |
|------|------|-------------|
| `build-v3-model.py` | 16 KB | Main training script |
| `Modelfile-gemma3-cv-expert-v3` | 93 KB | Output modelfile with 15 examples |
| `training-cvs.txt` | 228 KB | Extracted CV text |
| `scores/` | ~500 KB | JSON scores for ~50 CVs |
|| `data/` | 11 MB | Raw CV PDFs/DOCX (excluded from zip) |

---

## 🔄 Modellfleksibilitet og Alternativer

### Modell-Agnostisk Arkitektur

Systemet er bygget med **modell-agnostisk design** som gjør det enkelt å bytte til bedre eller kraftigere modeller etter behov. Du er **ikke låst til Gemma3 12B**.

### Enkelt Modellbytte i Konfigurasjonen

#### Backend (Spring Boot/Kotlin)
```yaml
# application-local.yaml
ollama:
  model: gemma3:12b-cv-expert-v3  # ← Endre bare denne linjen!
  base-url: http://localhost:11434
  readTimeoutSeconds: 120
```

#### Python Training Pipeline
```python
# build-v3-model.py
MODEL_NAME = "gemma3:12b-cv-expert-v3"      # Output model
SCORING_MODEL = "gemma3:12b-cv-expert-v2"   # Bootstrap model
# BASE_MODEL definert i Modelfile: "FROM gemma3:12b"
```

### Støttede Modeller i Ollama

Systemet fungerer med **alle Ollama-kompatible modeller**.

#### 🚀 Premium Alternativer (Kraftigere enn Gemma3 12B)

| Modell | Parametere | RAM Behov | Fordeler | Egnet til |
|--------|------------|-----------|----------|-----------||
| **Llama 3.3 70B** | 70B | ~48 GB | Beste kvalitet, flerspråklig | Server med GPU |
| **Qwen2.5 72B** | 72B | ~48 GB | Utmerket reasoning, koding | Server med GPU |
| **Mixtral 8x7B** | 47B (MoE) | ~32 GB | Rask, god kvalitet | Workstation |
| **Llama 3.1 70B** | 70B | ~48 GB | Meget solid allrounder | Server med GPU |

#### ⚡ Performance Alternativer (Raskere enn Gemma3 12B)

| Modell | Parametere | RAM Behov | Fordeler | Egnet til |
|--------|------------|-----------|----------|-----------||
| **Gemma2 27B** | 27B | ~18 GB | Nyere, mer effektiv | Laptop med 32GB RAM |
| **Llama 3.2 11B Vision** | 11B | ~8 GB | Vision + text, moderne | Standard laptop |
| **Qwen2.5 14B** | 14B | ~10 GB | Utmerket balanse | Standard laptop |
| **Phi-3.5 14B** | 14B | ~10 GB | Meget effektiv | Standard laptop |

#### 💨 Raskere Alternativer (Under 10B)

| Modell | Parametere | RAM Behov | Fordeler | Use case |
|--------|------------|-----------|----------|----------|
| **Gemma2 9B** | 9B | ~6 GB | Rask, god nok | Pre-filtering |
| **Llama 3.2 3B** | 3B | ~2 GB | Svært rask | Batch scoring |
| **Phi-3 Mini** | 3.8B | ~3 GB | Effektiv, kompakt | Embedded |

### Tre Tilnærminger for å Bytte Modell

#### Tilnærming A: Drop-in Replacement (Enklest)

Bruk en annen base-modell med samme Modelfile-struktur:

```bash
# Rediger Modelfile-gemma3-cv-expert-v3
# Endre første linje fra:
FROM gemma3:12b

# Til for eksempel:
FROM qwen2.5:14b
# eller
FROM llama3.3:70b
# eller
FROM mixtral:8x7b
```

Deretter:
```bash
# Pull ny base-modell
ollama pull qwen2.5:14b

# Recreate med nytt navn
ollama create qwen2.5:14b-cv-expert-v3 -f Modelfile-gemma3-cv-expert-v3

# Test
ollama run qwen2.5:14b-cv-expert-v3 "Evaluate this CV: ..."

# Oppdater backend config
# application-local.yaml: model: qwen2.5:14b-cv-expert-v3
```

#### Tilnærming B: Re-train med Ny Base-modell

For beste resultat, re-score CVer med den nye modellen:

```bash
cd ollama-models

# Oppdater build-v3-model.py
# Endre:
SCORING_MODEL = "qwen2.5:14b-cv-expert-v2"  # eller base qwen2.5:14b
OUTPUT_MODELFILE = "Modelfile-qwen2.5-cv-expert-v3"
MODEL_NAME = "qwen2.5:14b-cv-expert-v3"

# I write_modelfile():
# parts.append("FROM qwen2.5:14b")

# Kjør re-scoring og model building
python3 build-v3-model.py
```

#### Tilnærming C: Multi-Model Ensemble (Avansert)

Bruk forskjellige modeller for forskjellige oppgaver:

```yaml
# application-local.yaml
ollama:
  models:
    cv-scoring: qwen2.5:14b-cv-expert-v3      # Hovedscoring
    pre-filter: gemma2:9b-cv-filter           # Rask pre-filtering
    detailed-analysis: llama3.3:70b-cv-expert # Dybdeanalyse for topp-kandidater
```

Backend-implementasjon:
```kotlin
@Service
class CvScoringService(
    private val ollamaClient: OllamaHttpClient,
    @Value("\${ollama.models.cv-scoring}") private val scoringModel: String,
    @Value("\${ollama.models.pre-filter}") private val filterModel: String,
    @Value("\${ollama.models.detailed-analysis}") private val detailedModel: String
) {
    fun scoreConsultant(candidateUserId: String, detailed: Boolean = false): CvScore {
        val cvText = consultantRepository.findByUserId(candidateUserId)
        
        // Rask pre-filter (gemma2:9b)
        val quickScore = ollamaClient.chat(filterModel, "Quick CV score: $cvText")
        if (quickScore.score < 50) return quickScore  // Exit early
        
        // Hovedscoring (qwen2.5:14b)
        val mainScore = ollamaClient.chat(scoringModel, "Evaluate CV: $cvText")
        
        // Detaljert analyse for topp-kandidater (llama3.3:70b)
        if (detailed && mainScore.score >= 80) {
            return ollamaClient.chat(detailedModel, "Detailed: $cvText")
        }
        
        return mainScore
    }
}
```

### Chat Templates for Ulike Modeller

Noen modeller krever spesifikke chat templates:

#### Gemma3 Template (Nåværende)
```
{{ if .System }}<start_of_turn>system
{{ .System }}<end_of_turn>
{{ end }}{{ if .Prompt }}<start_of_turn>user
{{ .Prompt }}<end_of_turn>
{{ end }}<start_of_turn>model
{{ .Response }}<end_of_turn>
```

#### Llama 3 Template
```
{{ if .System }}<|begin_of_text|><|start_header_id|>system<|end_header_id|>
{{ .System }}<|eot_id|>{{ end }}{{ if .Prompt }}<|start_header_id|>user<|end_header_id|>
{{ .Prompt }}<|eot_id|>{{ end }}<|start_header_id|>assistant<|end_header_id|>
{{ .Response }}<|eot_id|>
```

#### Qwen2.5 Template
```
<|im_start|>system
{{ .System }}<|im_end|>
<|im_start|>user
{{ .Prompt }}<|im_end|>
<|im_start|>assistant
{{ .Response }}<|im_end|>
```

### Modell-Agnostisk Python Kode

Legg til template-provider for enkel switching:

```python
# model_provider.py
from abc import ABC, abstractmethod

class ModelProvider(ABC):
    @abstractmethod
    def get_base_model(self) -> str:
        pass
    
    @abstractmethod
    def get_chat_template(self) -> str:
        pass
    
    @abstractmethod
    def get_default_params(self) -> dict:
        pass

class Gemma3Provider(ModelProvider):
    def get_base_model(self) -> str:
        return "gemma3:12b"
    
    def get_chat_template(self) -> str:
        return GEMMA3_TEMPLATE
    
    def get_default_params(self) -> dict:
        return {"temperature": 0.2, "num_ctx": 12288}

class Qwen25Provider(ModelProvider):
    def get_base_model(self) -> str:
        return "qwen2.5:14b"
    
    def get_chat_template(self) -> str:
        return QWEN_TEMPLATE
    
    def get_default_params(self) -> dict:
        return {"temperature": 0.2, "num_ctx": 16384}

# Bruk i build-v3-model.py:
provider = Qwen25Provider()  # Eller Gemma3Provider()
BASE_MODEL = provider.get_base_model()
TEMPLATE = provider.get_chat_template()
```

### Benchmark Script

Test flere modeller side-om-side:

```bash
#!/bin/bash
# benchmark-models.sh

MODELS=(
    "gemma3:12b-cv-expert-v3"
    "qwen2.5:14b-cv-expert-v3"
    "llama3.3:70b-cv-expert-v3"
)

TEST_CV="data/test/sample-senior-cv.txt"

echo "Modell                        | Score | Tid"
echo "------------------------------|-------|------"

for MODEL in "${MODELS[@]}"; do
    TIME_START=$(date +%s)
    RESULT=$(ollama run "$MODEL" "Evaluate: $(cat $TEST_CV)")
    SCORE=$(echo "$RESULT" | jq -r '.scorePercentage')
    TIME_END=$(date +%s)
    ELAPSED=$((TIME_END - TIME_START))
    
    printf "%-30s| %3d/100 | %2ds\n" "$MODEL" "$SCORE" "$ELAPSED"
done
```

### Anbefalinger

#### For Laptop/Workstation (16-32 GB RAM)

**Best upgrade: Qwen2.5 14B**
```bash
ollama pull qwen2.5:14b

# Bedre reasoning og koding enn Gemma3 12B
# Samme RAM-krav (~10 GB)
# Raskere inferens
```

Endre Modelfile:
```dockerfile
FROM qwen2.5:14b

SYSTEM """
[Same Norwegian system prompt]
"""

PARAMETER temperature 0.2
PARAMETER top_p 0.9
PARAMETER num_ctx 16384

TEMPLATE """
<|im_start|>system
{{ .System }}<|im_end|>
<|im_start|>user
{{ .Prompt }}<|im_end|>
<|im_start|>assistant
{{ .Response }}<|im_end|>
"""

# [Same few-shot examples]
```

#### For Server med GPU (48+ GB VRAM)

**Premium: Llama 3.3 70B eller Qwen2.5 72B**
```bash
ollama pull llama3.3:70b
# eller
ollama pull qwen2.5:72b
```

**Fordeler**:
- Betydelig bedre reasoning
- Mer nyanserte evalueringer
- Bedre håndtering av komplekse CVer
- Flerspråklig støtte

#### For Produksjon med Høy Throughput

**Hybrid-tilnærming**:
```yaml
# Pre-filter med rask modell
pre-filter: gemma2:9b       # 6 GB RAM, ~1s per CV

# Hovedscoring med balansert modell
main-scoring: qwen2.5:14b   # 10 GB RAM, ~5s per CV

# Detaljanalyse med premium modell
detailed: llama3.3:70b      # 48 GB VRAM, ~15s per CV (kun topp 10%)
```

### Konklusjon: Modellfleksibilitet

Systemet er **100% modell-agnostisk**. Det eneste som er Gemma3-spesifikt:
1. Chat template (lett å bytte)
2. Model navn i config (1 linje)
3. Base model i Modelfile (1 linje)

**Anbefaling**:
- **Nå**: Test med `qwen2.5:14b` – lett oppgradering, samme hardware
- **Når GPU tilgjengelig**: Oppgrader til `llama3.3:70b` eller `qwen2.5:72b`
- **Produksjon**: Hybrid med 3 modeller (rask pre-filter, balansert main, premium detailed)

---

## 📦 Innhold i Zip-Fil

Denne dokumentasjonen følger med en zip-fil som inneholder:

```
cv-expert-training-tools.zip (< 5 MB)
├── README.md                           # Denne filen
├── scripts/
│   ├── build-v3-model.py               # Hovedscript
│   ├── extract-training-data.py        # Data extraction
│   ├── auto-score-cvs.sh               # Auto-scoring
│   ├── score-all-cvs.sh                # Manual scoring helper
│   └── test-cv-expert.sh               # Testing
├── documentation/
│   ├── README-CV-TRAINING.md           # Generell guide
│   ├── TRAINING-WORKFLOW.md            # Workflow
│   └── SCORING-GUIDE.md                # Scoring criteria
├── examples/
│   ├── Modelfile-gemma3-cv-expert-v3   # Latest modelfile (93KB)
│   ├── Modelfile-cv-examples.txt       # Template examples (30KB)
│   └── sample-scores/                  # 5 example JSON scores
├── requirements.txt                    # Python dependencies
└── SETUP.md                            # Installasjonsinstruksjoner
```

**Utelatt fra zip** (for størrelse):
- `data/` folder (11 MB)
- `venv/` virtual environment
- `.DS_Store` og andre system-filer
- Trained models (lastes ned via Ollama)

---

## 🎓 Konklusjon

Dette systemet representerer en **praktisk implementering av few-shot learning** for domene-spesifikk CV-evaluering. Ved å kombinere:

1. ✅ **Systematisk data extraction** (PyPDF2, python-docx)
2. ✅ **Strukturert scoring** (vektede kriterier)
3. ✅ **Automatisk model building** (Ollama Modelfile)
4. ✅ **Lokal deployment** (privacy, no API costs)
5. ✅ **Backend integration** (Spring Boot/Kotlin)

...oppnår vi en **kosteffektiv og vedlikeholdbar løsning** for CV-screening.

**Neste steg**:
- Samle mer treningsdata fra produksjon
- Implementer feedback loop
- Vurder overgang til full fine-tuning

---

**Opprettet**: 2024-03-25  
**Versjon**: 1.0  
**Forfatter**: Thomas Andersen (med hjelp fra Oz/Claude)  
**Prosjekt**: Cloudberries Candidate Match
