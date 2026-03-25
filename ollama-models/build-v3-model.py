#!/usr/bin/env python3
"""
Build gemma3:12b-cv-expert-v3 from all Flowcase CVs.

Pipeline:
  1. Score every .txt in data/flowcase-cv-catalog/text/ via Ollama REST API
     (resumable: skips already-scored CVs in scores/flowcase/)
  2. Select 15 diverse examples covering high / medium / low score range
  3. Write Modelfile-gemma3-cv-expert-v3
  4. Run `ollama create gemma3:12b-cv-expert-v3`

Run from the ollama-models/ directory:
  python3 build-v3-model.py

Requires Ollama to be running with gemma3:12b-cv-expert-v2 available.
"""

import json
import subprocess
import time
import urllib.request
import urllib.error
from pathlib import Path

# ── configuration ──────────────────────────────────────────────────────────────

SCRIPT_DIR       = Path(__file__).parent
TEXT_DIR         = SCRIPT_DIR / "data" / "flowcase-cv-catalog" / "text"
SCORES_DIR       = SCRIPT_DIR / "scores" / "flowcase"
OUTPUT_MODELFILE = SCRIPT_DIR / "Modelfile-gemma3-cv-expert-v3"
MODEL_NAME       = "gemma3:12b-cv-expert-v3"
SCORING_MODEL    = "gemma3:12b-cv-expert-v2"
OLLAMA_BASE_URL  = "http://localhost:11434"

MAX_CV_CHARS     = 6000    # trim CVs to this length to stay within context
MAX_EXAMPLES     = 15      # diverse few-shot examples in Modelfile
TIMEOUT_SECS     = 300     # 5 min per scoring call

# ── system prompt (improved v2) ────────────────────────────────────────────────

SYSTEM_PROMPT = """\
Du er en AI-assistent som evaluerer CV-er for Cloudberries, et konsulentselskap \
som spesialiserer seg på senior- og ekspertkonsulenter innen IT og teknologi.

Analysen skal være grundig og balansert, med hovedvekt på teknisk lederskap, \
arkitekturkompetanse og evnen til å omsette teknologi til forretningsverdi.

## Din ekspertise inkluderer:
- Vurdering av teknisk lederskap og arkitekturkompetanse
- Analyse av forretningsverdi og kompleksitetshåndtering
- Nordic labor market standards (Norge, Sverige, Danmark)
- Senior- og ekspertkonsulent-profiler
- Moderne teknologi: KI, sky, DevOps, Kubernetes
- Evaluering av prosjektbeskrivelser og rollebidrag

## Evalueringskriterier (Vektet Scoring):

1. **Struktur og Profesjonalitet** (Vekt 1.0)
   - Logisk oppbygning, klarhet, profesjonell tone
   - Fravær av skrivefeil, polert inntrykk

2. **Prosjekt- og Rollebeskrivelser** (Vekt 2.5) - VIKTIGST
   - Forklares formålet og forretningsverdien?
   - Er rolle, ansvar og konkrete bidrag tydelige?

3. **Teknisk Dybde og Anvendelse** (Vekt 2.0)
   - Går utover lister av teknologier?
   - Viser hvordan og hvorfor teknologier ble brukt?
   - Gjentatt bruk i relevante prosjekter

4. **Lederskap, Mentoring og Faglig Initiativ** (Vekt 2.5) - VIKTIGST
   - Roller som arkitekt, tech lead, fagansvarlig, mentor
   - Kunnskapsdeling: foredrag, workshops, blogg, rammeverk

5. **KI-kompetanse og Moderne Teknologi** (Vekt 2.0)
   - Praktisk erfaring med KI (LLM, RAG)
   - Skyplattformer (Azure, AWS, GCP)
   - DevOps, CI/CD, Kubernetes

## Totalscore beregning:
Totalscore = ((Score1 × 1.0) + (Score2 × 2.5) + (Score3 × 2.0) + (Score4 × 2.5) + (Score5 × 2.0)) / 10.0

## Retningslinjer for scoring:
- 90+: Eksepsjonell kombinasjon av teknisk dybde, lederskap og forretningsforståelse
- 80-89: Sterk senior-profil med klare styrker
- 70-79: Solid profil med noen forbedringsområder
- 50-69: Midlere profil med vesentlige mangler
- Under 50: Junior-profil eller svak presentasjon

## Output format:
Returner KUN gyldig JSON med denne strukturen:
{
  "summary": "Konsis oppsummering av kandidatens profil",
  "strengths": ["Styrke 1", "Styrke 2", "Styrke 3"],
  "improvements": ["Forbedring 1", "Forbedring 2", "Forbedring 3"],
  "scoreBreakdown": {
    "structureAndProfessionalism": {"score": 0-100, "justification": "..."},
    "projectAndRoleDescriptions": {"score": 0-100, "justification": "..."},
    "technicalDepth": {"score": 0-100, "justification": "..."},
    "leadershipAndInitiative": {"score": 0-100, "justification": "..."},
    "aiAndModernTech": {"score": 0-100, "justification": "..."}
  },
  "scorePercentage": 0-100
}

Din respons skal alltid være på norsk og kun inneholde gyldig JSON.\
"""

MODEL_PARAMS = """\
PARAMETER temperature 0.2
PARAMETER top_p 0.9
PARAMETER top_k 40
PARAMETER num_ctx 12288
PARAMETER repeat_penalty 1.1\
"""

GEMMA_TEMPLATE = """\
{{ if .System }}<start_of_turn>system
{{ .System }}<end_of_turn>
{{ end }}{{ if .Prompt }}<start_of_turn>user
{{ .Prompt }}<end_of_turn>
{{ end }}<start_of_turn>model
{{ .Response }}<end_of_turn>\
"""

# Synthetic low-score anchor (always included first)
JANE_SMITH_BLOCK = """\
MESSAGE user \"\"\"Evaluate this CV:

Jane Smith
Junior utvikler
2 år erfaring
Fullstack-utvikler hos Agency (2023-2024): WordPress, PHP
Intern hos LocalCompany (2022): HTML, CSS, JavaScript
Skills: WordPress, PHP, HTML, CSS, JavaScript, jQuery\"\"\"

MESSAGE assistant \"\"\"{
  "scorePercentage": 32,
  "summary": "Junior-profil med begrenset erfaring (2 år totalt). Teknologistakken er utdatert for moderne utviklerroller. Mangler dybde i moderne rammeverk.",
  "strengths": [
    "Nylig i startfasen av karrieren",
    "Fullstack-bevissthet (frontend og backend)",
    "Grunnlag i web-fundamentaler (HTML, CSS, JavaScript)"
  ],
  "improvements": [
    "Svært begrenset profesjonell erfaring (kun 2 år)",
    "Teknologistakken er utdatert – WordPress/PHP begrenser muligheter",
    "Mangler moderne JavaScript-rammeverk (React, Vue, Angular)",
    "Ingen sky-, DevOps- eller containeriserings-erfaring",
    "Ingen demonstrert lederskap eller senior-ansvar"
  ],
  "scoreBreakdown": {
    "structureAndProfessionalism": {"score": 60, "justification": "Enkel struktur men mangler detaljer og profesjonell dybde"},
    "projectAndRoleDescriptions": {"score": 25, "justification": "Begrenset relevant erfaring, utdatert stack for moderne senior-roller"},
    "technicalDepth": {"score": 30, "justification": "Grunnleggende kompetanse men mangler moderne teknologier og dybde"},
    "leadershipAndInitiative": {"score": 10, "justification": "Ingen lederskap eller faglig initiativ dokumentert"},
    "aiAndModernTech": {"score": 5, "justification": "Ingen sky, AI, eller DevOps-erfaring"}
  }
}\"\"\"\
"""


# ── Ollama API ─────────────────────────────────────────────────────────────────

def ollama_chat(model: str, user_message: str, timeout: int = TIMEOUT_SECS) -> str:
    """Call Ollama chat API. Returns the assistant message content."""
    payload = json.dumps({
        "model": model,
        "messages": [{"role": "user", "content": user_message}],
        "stream": False,
        "options": {"temperature": 0.2, "num_ctx": 8192},
    }).encode()

    req = urllib.request.Request(
        f"{OLLAMA_BASE_URL}/api/chat",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        result = json.loads(resp.read().decode())
        return result["message"]["content"]


def extract_json(raw: str) -> dict:
    """Extract the first JSON object from a model response."""
    raw = raw.strip()
    # Strip markdown code fences
    if raw.startswith("```"):
        lines = [l for l in raw.split("\n") if not l.strip().startswith("```")]
        raw = "\n".join(lines).strip()
    start = raw.find("{")
    end = raw.rfind("}") + 1
    if start >= 0 and end > start:
        return json.loads(raw[start:end])
    raise ValueError(f"No JSON found in response: {raw[:300]!r}")


# ── scoring ────────────────────────────────────────────────────────────────────

REQUIRED_BREAKDOWN_KEYS = [
    "structureAndProfessionalism",
    "projectAndRoleDescriptions",
    "technicalDepth",
    "leadershipAndInitiative",
    "aiAndModernTech",
]


def normalise_score(score: dict) -> dict:
    """Ensure the score dict has all required keys and correct structure."""
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


def score_cv_text(cv_text: str) -> dict:
    prompt = f"Evaluate this CV:\n\n{cv_text[:MAX_CV_CHARS]}"
    raw = ollama_chat(SCORING_MODEL, prompt)
    return normalise_score(extract_json(raw))


def score_all(text_files: list) -> dict:
    """Score all CVs, loading cached results from disk when available."""
    SCORES_DIR.mkdir(parents=True, exist_ok=True)
    results: dict = {}
    total = len(text_files)

    for i, txt_path in enumerate(text_files, 1):
        score_path = SCORES_DIR / (txt_path.stem + ".json")

        if score_path.exists():
            with open(score_path, encoding="utf-8") as f:
                data = json.load(f)
            results[txt_path.stem] = normalise_score(data)
            pct = results[txt_path.stem].get("scorePercentage", "?")
            print(f"  [{i:3d}/{total}] ✓ cached   {txt_path.name:<65}  score={pct}")
            continue

        cv_text = txt_path.read_text(encoding="utf-8")
        print(f"  [{i:3d}/{total}] scoring   {txt_path.name:<65} ", end="", flush=True)
        t0 = time.time()

        try:
            score = score_cv_text(cv_text)
            elapsed = time.time() - t0
            pct = score.get("scorePercentage", "?")
            print(f" {pct:>5}/100  ({elapsed:.0f}s)")

            results[txt_path.stem] = score
            with open(score_path, "w", encoding="utf-8") as f:
                json.dump(score, f, ensure_ascii=False, indent=2)

        except Exception as e:
            elapsed = time.time() - t0
            print(f" ❌ FAILED ({elapsed:.0f}s): {e}")

    return results


# ── example selection ──────────────────────────────────────────────────────────

def pick_spread(lst: list, k: int) -> list:
    """Pick k evenly-spaced items from lst."""
    if not lst or k <= 0:
        return []
    if len(lst) <= k:
        return lst[:]
    step = len(lst) / k
    return [lst[round(i * step)] for i in range(k)]


def select_examples(text_files: list, scores: dict, n: int = MAX_EXAMPLES) -> list:
    """Select n diverse examples spanning the full score range."""
    entries = []
    for txt in text_files:
        s = scores.get(txt.stem)
        if s and isinstance(s.get("scorePercentage"), (int, float)):
            entries.append((txt, s))

    entries.sort(key=lambda x: x[1]["scorePercentage"], reverse=True)

    if len(entries) <= n:
        return entries

    high = [e for e in entries if e[1]["scorePercentage"] >= 80]
    mid  = [e for e in entries if 55 <= e[1]["scorePercentage"] < 80]
    low  = [e for e in entries if e[1]["scorePercentage"] < 55]

    n_high = max(1, n // 3)
    n_low  = max(1, n // 5)
    n_mid  = n - n_high - n_low

    selected = (
        pick_spread(high, n_high)
        + pick_spread(mid, n_mid)
        + pick_spread(low, n_low)
    )

    # Deduplicate, preserving order
    seen: set = set()
    unique = []
    for e in selected:
        key = e[0].stem
        if key not in seen:
            seen.add(key)
            unique.append(e)

    return unique[:n]


# ── Modelfile building ─────────────────────────────────────────────────────────

def build_cv_block(cv_text: str, score: dict, label: str) -> str:
    """Produce one MESSAGE user / MESSAGE assistant pair."""
    score_str = json.dumps(score, ensure_ascii=False, indent=2)
    excerpt = cv_text[:MAX_CV_CHARS].replace('"""', '"')
    return (
        f"# {label}\n"
        f"MESSAGE user \"\"\"Evaluate this CV:\n\n{excerpt}\"\"\"\n\n"
        f"MESSAGE assistant \"\"\"{score_str}\"\"\""
    )


def write_modelfile(examples: list) -> None:
    parts = [
        f"# Modelfile for {MODEL_NAME}",
        f"# Training set: {len(examples)} real Flowcase CVs + 1 synthetic low-score anchor",
        "# Generated by build-v3-model.py",
        "",
        "FROM gemma3:12b",
        "",
        'SYSTEM """',
        SYSTEM_PROMPT,
        '"""',
        "",
        MODEL_PARAMS,
        "",
        'TEMPLATE """',
        GEMMA_TEMPLATE,
        '"""',
        "",
        "# ── Synthetic low-score anchor ──────────────────────────────────────────────",
        JANE_SMITH_BLOCK,
        "",
        "# ── Real Flowcase consultant CVs ────────────────────────────────────────────",
    ]

    for txt_path, score in examples:
        pct = score.get("scorePercentage", "?")
        cv_text = txt_path.read_text(encoding="utf-8")
        parts.append("")
        parts.append(build_cv_block(cv_text, score, f"{txt_path.name}  score={pct}"))

    OUTPUT_MODELFILE.write_text("\n".join(parts), encoding="utf-8")


# ── main ───────────────────────────────────────────────────────────────────────

def main() -> None:
    sep = "=" * 66
    print(f"\n{sep}")
    print(f"  Build {MODEL_NAME}")
    print(f"  Source CVs : {TEXT_DIR}")
    print(f"  Scoring via: {SCORING_MODEL}")
    print(f"{sep}\n")

    text_files = sorted(TEXT_DIR.glob("*.txt"))
    if not text_files:
        print(f"❌  No .txt files found in {TEXT_DIR}")
        return
    print(f"  Found {len(text_files)} CV text files\n")

    # ── Step 1: score ──────────────────────────────────────────────────────────
    print("STEP 1: Scoring CVs  (resumable – Ctrl-C safe)\n")
    scores = score_all(text_files)
    scored = sum(1 for s in scores.values() if isinstance(s.get("scorePercentage"), (int, float)))
    print(f"\n  Scored {scored}/{len(text_files)} CVs successfully.\n")

    # ── Step 2: select ────────────────────────────────────────────────────────
    examples = select_examples(text_files, scores, MAX_EXAMPLES)
    print(f"STEP 2: Selected {len(examples)} examples for Modelfile:\n")
    for txt_path, sc in examples:
        pct = sc.get("scorePercentage", "?")
        print(f"  {pct:>5}  {txt_path.name}")

    # ── Step 3: write Modelfile ───────────────────────────────────────────────
    print(f"\nSTEP 3: Writing {OUTPUT_MODELFILE.name} …")
    write_modelfile(examples)
    size_kb = OUTPUT_MODELFILE.stat().st_size // 1024
    print(f"  Done: {OUTPUT_MODELFILE}  ({size_kb} KB)\n")

    # ── Step 4: create model ──────────────────────────────────────────────────
    print(f"STEP 4: Creating Ollama model  {MODEL_NAME}\n")
    result = subprocess.run(
        ["ollama", "create", MODEL_NAME, "-f", str(OUTPUT_MODELFILE)],
        capture_output=False,
    )
    if result.returncode == 0:
        print(f"\n✅  Model {MODEL_NAME} created successfully!")
        print(f"    Test: ollama run {MODEL_NAME} \"Evaluate this CV: ...\"\n")
    else:
        print(f"\n❌  ollama create failed (exit code {result.returncode})")


if __name__ == "__main__":
    main()
