#!/bin/bash
# Auto-score CVs using Gemma3 with the Norwegian prompt template
# This creates initial scores that you should review and refine

set -e

CV_EXAMPLES_FILE="Modelfile-cv-examples.txt"
OUTPUT_FILE="Modelfile-auto-scored.txt"
MODEL="gemma3:12b-cv-expert"

# Norwegian CV evaluation prompt (from CvReviewPromptTemplate.kt)
read -r -d '' PROMPT_TEMPLATE << 'EOF' || true
# 🤖 CV-Vurdering (Analytisk Modell)

**Vurderingsoppgave: Analyse av konsulent-CV**

Du er en AI-assistent som skal hjelpe til med å evaluere CV-er for et konsulentselskap som spesialiserer seg på senior- og ekspertkonsulenter. Analysen skal være grundig og balansert, med hovedvekt på teknisk lederskap, arkitekturkompetanse og evnen til å omsette teknologi til forretningsverdi.

Utfør følgende oppgaver nøyaktig i rekkefølge:

**STEG 1: Kvalitativ Analyse**

1. **Sammendrag og Helhetsinntrykk:**
   Gi en konsis oppsummering av kandidatens profil.

2. **Fremtredende Styrker (Top 3-5):**
   * Lederskap og Initiativ
   * Moderne Teknologikompetanse
   * Forretningsforståelse
   * Kompleksitetshåndtering

3. **Forbedringsområder (Top 3-5):**
   Gi konkrete og handlingsorienterte forslag.

**STEG 2: Analytisk Scoring (0-100 per kriterium)**

1. Struktur og Profesjonalitet (Vekt 1.0)
2. Prosjekt- og Rollebeskrivelser (Vekt 2.5)
3. Teknisk Dybde og Anvendelse (Vekt 2.0)
4. Lederskap, Mentoring og Faglig Initiativ (Vekt 2.5)
5. KI-kompetanse og Moderne Teknologi (Vekt 2.0)

**STEG 3: Totalscore**
Totalscore = (Score1*1.0 + Score2*2.5 + Score3*2.0 + Score4*2.5 + Score5*2.0) / 10.0

**Format: Returner KUN gyldig JSON:**
{
  "summary": "...",
  "strengths": ["...", "...", "..."],
  "improvements": ["...", "...", "..."],
  "scoreBreakdown": {
    "structureAndReadability": {"score": 0, "justification": "..."},
    "contentAndRelevance": {"score": 0, "justification": "..."},
    "quantificationAndResults": {"score": 0, "justification": "..."},
    "technicalDepth": {"score": 0, "justification": "..."},
    "languageAndProfessionalism": {"score": 0, "justification": "..."}
  },
  "scorePercentage": 0
}
EOF

echo "🤖 Auto-scoring CVs using Norwegian template..."
echo "Model: $MODEL"
echo "Output: $OUTPUT_FILE"
echo ""

# Check if model exists
if ! ollama list | grep -q "$MODEL"; then
    echo "⚠️  Model $MODEL not found. Using base model gemma3:12b instead."
    MODEL="gemma3:12b"
fi

# Extract CV names from the examples file
# Format: "# From: filename.pdf"
cv_files=$(grep "^# From:" "$CV_EXAMPLES_FILE" | sed 's/^# From: //' | head -5)

echo "📋 Scoring first 5 CVs for initial training..."
echo ""

> "$OUTPUT_FILE"  # Clear output file

count=0
for cv_file in $cv_files; do
    count=$((count + 1))
    echo "[$count/5] Scoring: $cv_file"
    
    # Extract CV text from examples file (between MESSAGE user and MESSAGE assistant)
    cv_text=$(sed -n "/# From: $cv_file/,/MESSAGE assistant/p" "$CV_EXAMPLES_FILE" | \
              sed -n '/MESSAGE user/,/MESSAGE assistant/p' | \
              sed '1d;$d' | \
              sed 's/^"""//' | sed 's/"""$//')
    
    # Score using Ollama with Norwegian prompt
    echo "   Sending to $MODEL..."
    
    full_prompt="${PROMPT_TEMPLATE}

---

CV to evaluate:
${cv_text}
"
    
    # Get score from model (with timeout)
    score_json=$(echo "$full_prompt" | timeout 180s ollama run "$MODEL" 2>/dev/null || echo '{"error": "timeout or failed"}')
    
    # Clean up JSON response (remove markdown formatting if present)
    score_json=$(echo "$score_json" | sed 's/```json//g' | sed 's/```//g' | grep -v '^#' | jq -c '.' 2>/dev/null || echo '{"scorePercentage": 70, "summary": "Auto-scored", "strengths": ["N/A"], "improvements": ["Review manually"]}')
    
    # Extract key fields for Modelfile format
    score=$(echo "$score_json" | jq -r '.scorePercentage // 70')
    summary=$(echo "$score_json" | jq -r '.summary // "Auto-generated summary"')
    strengths=$(echo "$score_json" | jq -r '.strengths // ["N/A"]' | jq -c '.')
    improvements=$(echo "$score_json" | jq -r '.improvements // ["Review manually"]' | jq -c '.')
    
    echo "   Score: $score/100"
    
    # Append to output file in Modelfile format
    cat >> "$OUTPUT_FILE" << EXAMPLE

# From: $cv_file (Auto-scored: $score/100)

MESSAGE user """Evaluate this CV:

${cv_text}
"""

MESSAGE assistant """{
  "scorePercentage": $score,
  "summary": "$summary",
  "strengths": $strengths,
  "improvements": $improvements
}"""

EXAMPLE
    
    echo "   ✅ Done"
    echo ""
done

echo ""
echo "✅ Auto-scoring complete!"
echo ""
echo "📄 Results saved to: $OUTPUT_FILE"
echo ""
echo "⚠️  IMPORTANT: Review and refine these scores manually before adding to Modelfile!"
echo ""
echo "Next steps:"
echo "1. Review $OUTPUT_FILE"
echo "2. Refine scores, summaries, strengths, and improvements"
echo "3. Copy refined examples to Modelfile-gemma3-cv-expert"
echo "4. Run: ollama create gemma3:12b-cv-expert -f Modelfile-gemma3-cv-expert --force"
