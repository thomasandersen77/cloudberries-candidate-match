#!/usr/bin/env python3
"""
Build Modelfile-gemma3-cv-expert-v2 using all scored CVs and create the model.
Model name: gemma3:12b-cv-expert-v2
"""

import os
import json
import subprocess
from pathlib import Path

DATA_DIR    = Path(__file__).parent / "data"
SCORES_DIR  = Path(__file__).parent / "scores"
OUTPUT_FILE = Path(__file__).parent / "Modelfile-gemma3-cv-expert-v2"
MODEL_NAME  = "gemma3:12b-cv-expert-v2"

# ── text extraction helpers ────────────────────────────────────────────────────

def extract_pdf(path: Path, max_chars: int = 3500) -> str:
    from pypdf import PdfReader
    reader = PdfReader(str(path))
    text = ""
    for page in reader.pages[:6]:
        text += page.extract_text() or ""
    return text[:max_chars]

def extract_docx(path: Path, max_chars: int = 3500) -> str:
    from docx import Document
    doc = Document(str(path))
    text = "\n".join(p.text for p in doc.paragraphs if p.text.strip())
    return text[:max_chars]

def extract_cv(path: Path) -> str:
    if path.suffix.lower() == ".pdf":
        return extract_pdf(path)
    elif path.suffix.lower() == ".docx":
        return extract_docx(path)
    return ""

# ── score file lookup ──────────────────────────────────────────────────────────

def find_score(cv_path: Path) -> dict | None:
    # score files follow the pattern: <cv_filename>.json.json
    score_file = SCORES_DIR / (cv_path.name + ".json.json")
    if not score_file.exists():
        # fallback: strip extension + .json.json
        score_file = SCORES_DIR / (cv_path.stem + ".json.json")
    if score_file.exists():
        with open(score_file) as f:
            return json.load(f)
    return None

# ── Modelfile content ──────────────────────────────────────────────────────────

SYSTEM_PROMPT = """\
Du er en AI-assistent som evaluerer CV-er for Cloudberries, et konsulentselskap som spesialiserer seg på senior- og ekspertkonsulenter innen IT og teknologi.

Analysen skal være grundig og balansert, med hovedvekt på teknisk lederskap, arkitekturkompetanse og evnen til å omsette teknologi til forretningsverdi.

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
- En score på 90+ er kun for kandidater med eksepsjonell kombinasjon av teknisk dybde, lederskap og forretningsforståelse
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
PARAMETER num_ctx 8192
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

# synthetic low-score anchor example
JANE_SMITH_EXAMPLE = """MESSAGE user \"\"\"Evaluate this CV:

Jane Smith
Junior utvikler
2 år erfaring
Fullstack-utvikler hos Agency (2023-2024): WordPress, PHP
Intern hos LocalCompany (2022): HTML, CSS, JavaScript
Skills: WordPress, PHP, HTML, CSS, JavaScript, jQuery
\"\"\"

MESSAGE assistant \"\"\"{
  "scorePercentage": 32,
  "summary": "Junior-profil med begrenset erfaring (2 år totalt, 1 år profesjonelt). Teknologistakken er utdatert for moderne utviklerroller. Mangler dybde i moderne rammeverk og profesjonell erfaring.",
  "strengths": [
    "Nylig i startfasen av karrieren",
    "Fullstack-bevissthet som dekker både frontend og backend",
    "Grunnlag i web-fundamentaler (HTML, CSS, JavaScript)"
  ],
  "improvements": [
    "Svært begrenset profesjonell erfaring (kun 2 år)",
    "Teknologistakken er utdatert - WordPress/PHP-fokus begrenser muligheter",
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
}\"\"\"
"""

def build_example_block(cv_text: str, score_data: dict, cv_name: str) -> str:
    score_str = json.dumps(score_data, ensure_ascii=False, indent=2)
    return f"""MESSAGE user \"\"\"Evaluate this CV:

{cv_text}
\"\"\"

MESSAGE assistant \"\"\"{score_str}\"\"\"
"""

# ── main ───────────────────────────────────────────────────────────────────────

def main():
    cv_files = sorted([
        f for f in DATA_DIR.iterdir()
        if f.suffix.lower() in (".pdf", ".docx")
        and "templates/default" not in str(f)
    ])

    print(f"Found {len(cv_files)} CVs in {DATA_DIR}")

    examples = []
    for cv_path in cv_files:
        score = find_score(cv_path)
        if score is None:
            print(f"  ⚠️  No score found for {cv_path.name}, skipping")
            continue
        try:
            cv_text = extract_cv(cv_path)
        except Exception as e:
            print(f"  ⚠️  Failed to extract {cv_path.name}: {e}, skipping")
            continue
        if not cv_text.strip():
            print(f"  ⚠️  Empty text for {cv_path.name}, skipping")
            continue

        # Ensure scoreBreakdown keys match the new schema
        if "scoreBreakdown" in score:
            old = score["scoreBreakdown"]
            # remap legacy keys if needed
            mapping = {
                "structureAndReadability":   "structureAndProfessionalism",
                "contentAndRelevance":       "projectAndRoleDescriptions",
                "quantificationAndResults":  "projectAndRoleDescriptions",
                "languageAndProfessionalism":"structureAndProfessionalism",
            }
            new_bd: dict = {}
            for k, v in old.items():
                new_key = mapping.get(k, k)
                if new_key not in new_bd:
                    new_bd[new_key] = v
            # ensure all required keys exist
            for req in ["structureAndProfessionalism","projectAndRoleDescriptions",
                        "technicalDepth","leadershipAndInitiative","aiAndModernTech"]:
                if req not in new_bd:
                    new_bd[req] = {"score": score.get("scorePercentage", 75),
                                   "justification": "Ikke vurdert separat"}
            score["scoreBreakdown"] = new_bd

        print(f"  ✅ {cv_path.name:60s}  score={score.get('scorePercentage','?')}")
        examples.append(build_example_block(cv_text, score, cv_path.name))

    # write Modelfile
    lines = [
        f"# Modelfile for {MODEL_NAME}",
        f"# Training set: {len(examples)} real Cloudberries consultant CVs + 1 synthetic low-score anchor",
        f"# Generated by build-v2-model.py",
        "",
        "FROM gemma3:12b",
        "",
        "SYSTEM \"\"\"",
        SYSTEM_PROMPT,
        "\"\"\"",
        "",
        MODEL_PARAMS,
        "",
        "TEMPLATE \"\"\"",
        GEMMA_TEMPLATE,
        "\"\"\"",
        "",
        "# ── Synthetic low-score anchor (score distribution anchor) ────────────────",
        JANE_SMITH_EXAMPLE,
        "# ── Real Cloudberries consultant CVs ─────────────────────────────────────",
    ]
    lines += examples

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    print(f"\n📄 Modelfile written: {OUTPUT_FILE}")
    print(f"   Examples: {len(examples)} real + 1 synthetic = {len(examples)+1} total")
    print()
    print(f"🚀 Creating model {MODEL_NAME} ...")
    result = subprocess.run(
        ["ollama", "create", MODEL_NAME, "-f", str(OUTPUT_FILE)],
        capture_output=False
    )
    if result.returncode == 0:
        print(f"\n✅ Model {MODEL_NAME} created successfully!")
    else:
        print(f"\n❌ ollama create failed (exit {result.returncode})")

if __name__ == "__main__":
    main()
