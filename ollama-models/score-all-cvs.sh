#!/bin/bash
# Score all CVs in the data folder using gemma3:12b-cv-expert

set -e

MODEL="gemma3:12b-cv-expert"
DATA_DIR="data"
OUTPUT_DIR="scores"
THOMAS_CV="Thomas-Andersen_CV_Oktober.docx"
PYTHON="venv/bin/python3"

mkdir -p "$OUTPUT_DIR"

echo "🤖 Scoring all CVs in $DATA_DIR using $MODEL"
echo ""

# Check if model exists
if ! ollama list | grep -q "$MODEL"; then
    echo "❌ Model $MODEL not found. Please create it first."
    exit 1
fi

# Find all PDF and DOCX files (excluding template files)
cv_files=$(find "$DATA_DIR" -type f \( -name "*.pdf" -o -name "*.docx" \) | grep -v "templates/default.docx" | sort)

total=$(echo "$cv_files" | wc -l | tr -d ' ')
echo "📋 Found $total CVs to score"
echo ""

count=0
thomas_score=""

# Norwegian scoring prompt
read -r -d '' PROMPT << 'EOF' || true
Vurder denne CV-en basert på følgende kriterier:
1. Teknisk dybde og erfaring
2. Lederskap og initiativ
3. Moderne teknologikompetanse
4. Forretningsforståelse
5. CV-kvalitet og presentasjon

Returner kun gyldig JSON i dette formatet:
{
  "scorePercentage": <tall 0-100>,
  "summary": "<kort sammendrag på norsk>",
  "strengths": ["<styrke 1>", "<styrke 2>", "<styrke 3>"],
  "improvements": ["<forbedring 1>", "<forbedring 2>", "<forbedring 3>"]
}

CV:
EOF

for cv_path in $cv_files; do
    count=$((count + 1))
    cv_name=$(basename "$cv_path")
    
    echo "[$count/$total] Scoring: $cv_name"
    
    # Extract text from PDF or DOCX
    if [[ "$cv_path" == *.pdf ]]; then
        cv_text=$($PYTHON -c "
import sys
from pypdf import PdfReader
try:
    reader = PdfReader('$cv_path')
    text = ''
    for page in reader.pages[:5]:  # First 5 pages
        text += page.extract_text()
    print(text[:4000])  # Limit to 4000 chars
except Exception as e:
    print(f'Error: {e}', file=sys.stderr)
    sys.exit(1)
" 2>/dev/null || echo "Failed to extract PDF")
    else
        cv_text=$($PYTHON -c "
import sys
from docx import Document
try:
    doc = Document('$cv_path')
    text = '\n'.join([p.text for p in doc.paragraphs[:100]])
    print(text[:4000])
except Exception as e:
    print(f'Error: {e}', file=sys.stderr)
    sys.exit(1)
" 2>/dev/null || echo "Failed to extract DOCX")
    fi
    
    if [[ "$cv_text" == "Failed to extract"* ]]; then
        echo "   ⚠️  Skipping (extraction failed)"
        echo ""
        continue
    fi
    
    # Build full prompt
    full_prompt="${PROMPT}

${cv_text}"
    
    # Score using Ollama
    echo "   Analyzing with $MODEL..."
    score_json=$(echo "$full_prompt" | timeout 240s ollama run "$MODEL" 2>/dev/null || echo '{"scorePercentage": 0, "summary": "Timeout", "strengths": [], "improvements": []}')
    
    # Clean JSON (remove markdown)
    score_json=$(echo "$score_json" | sed 's/```json//g' | sed 's/```//g' | jq -c '.' 2>/dev/null || echo '{"scorePercentage": 0, "summary": "Parse error", "strengths": [], "improvements": []}')
    
    # Extract score
    score=$(echo "$score_json" | jq -r '.scorePercentage // 0')
    
    echo "   Score: $score/100"
    
    # Save to file
    output_file="$OUTPUT_DIR/${cv_name%.pdf}.json"
    output_file="${output_file%.docx}.json"
    echo "$score_json" | jq '.' > "$output_file"
    
    # Check if this is Thomas's CV
    if [[ "$cv_name" == "$THOMAS_CV" ]]; then
        thomas_score="$score"
        echo "   ⭐️ THOMAS ANDERSEN CV SCORED: $thomas_score/100"
    fi
    
    echo "   ✅ Saved to $output_file"
    echo ""
done

echo ""
echo "✅ Scoring complete!"
echo "📊 Results saved to $OUTPUT_DIR/"
echo ""

if [[ -n "$thomas_score" ]]; then
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "⭐️  THOMAS ANDERSEN CV SCORE: $thomas_score/100"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "Full results:"
    cat "$OUTPUT_DIR/Thomas-Andersen_CV_Oktober.json" | jq '.'
else
    echo "⚠️  Thomas-Andersen_CV_Oktober.docx was not found or scored"
fi
