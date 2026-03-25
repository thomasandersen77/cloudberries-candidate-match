# Complete Training Workflow for Gemma3 CV Expert

## ✅ Step 1: Data Extraction (COMPLETED)

```bash
cd /Users/tandersen/git/cloudberries-candidate-match/ollama-models
source venv/bin/activate
python extract-training-data.py --cv-folder data --output training-cvs.txt --modelfile-output Modelfile-cv-examples.txt
```

**Result**: 
- ✅ 13 CVs extracted successfully
- ✅ `training-cvs.txt` - Full text from all CVs
- ✅ `Modelfile-cv-examples.txt` - Templates ready for scoring

## 📝 Step 2: Manual Scoring (DO THIS NEXT)

### Quick Start - Score 5 CVs (1-2 hours)

1. **Open the template file**:
   ```bash
   code Modelfile-cv-examples.txt
   # or nano, vim, etc.
   ```

2. **Choose 5 diverse CVs to score first**:
   - 1 exceptional senior (90+)
   - 1 strong senior (75-89)
   - 1 mid-level (60-74)
   - 1 junior-mid (40-59)
   - 1 junior/entry (20-39)

3. **For each CV, replace placeholders**:
   - `[SCORE_HERE]` → actual number (e.g., 92)
   - `[SUMMARY_HERE]` → 2-3 sentence evaluation
   - `[STRENGTH_1]`, etc. → specific strengths from CV (3-7 items)
   - `[IMPROVEMENT_1]`, etc. → actionable improvements (3-5 items)

4. **Use the scoring guide**: See `SCORING-GUIDE.md` for examples

5. **Save your scored examples** to a new file:
   ```bash
   # Copy the scored examples you completed
   # Save as: Modelfile-scored-examples.txt
   ```

### Example of a Completed Scoring

```
MESSAGE user """Evaluate this CV:

Joachim Lous
Seniorkonsulent
Experienced developer and tech lead
Java and Kotlin experience from large public systems
Several years at Skatteetaten (Norwegian Tax Administration)
Microservices architecture on-prem, agile methods
DevOps, CI/CD, Azure cloud experience, AWS certified
Roles: Scrum Master, Solution Architect
Project: "Avgift på fisk" - solo technical lead, full delivery
"""

MESSAGE assistant """{
  "scorePercentage": 87,
  "summary": "Highly experienced developer and technical leader with deep expertise in Java/Kotlin and enterprise microservices. Proven track record in critical public sector systems (Skatteetaten) with strong DevOps and cloud capabilities. Excellent combination of technical depth, leadership, and ability to deliver complete solutions independently.",
  "strengths": [
    "Extensive experience with large-scale, business-critical public sector systems",
    "Strong technical leadership as tech lead, Scrum Master, and Solution Architect",
    "Modern tech stack: Java/Kotlin, Spring Boot, microservices, Docker, Kafka",
    "Cloud experience with Azure and AWS certification",
    "Proven ability to deliver complete projects independently ('Avgift på fisk')",
    "Deep understanding of agile methodologies and business value focus",
    "Strong communication skills (written, oral, visual) and solution-oriented mindset",
    "DevOps and CI/CD expertise for complete software delivery lifecycle"
  ],
  "improvements": [
    "Frontend skills not prominently mentioned - may limit full-stack opportunities",
    "No specific mention of Kubernetes or container orchestration at scale",
    "Could benefit from additional cloud certifications beyond AWS",
    "Limited international or private sector experience (mostly Norwegian public sector)",
    "No mention of specific testing practices or TDD"
  ]
}"""
```

## 🔧 Step 3: Add Scored Examples to Modelfile

1. **Open the main Modelfile**:
   ```bash
   nano Modelfile-gemma3-cv-expert
   ```

2. **Add your scored examples at the end** (before the file closes):
   ```
   # ... existing content ...
   
   # Examples from Cloudberries CV database
   [Paste your scored examples here]
   ```

3. **Keep the existing 2 examples** in the Modelfile - they provide baseline guidance

## 🚀 Step 4: Recreate the Model

```bash
cd /Users/tandersen/git/cloudberries-candidate-match

# Create/update the CV expert model
ollama create gemma3:12b-cv-expert \
  -f ollama-models/Modelfile-gemma3-cv-expert \
  --force

# Expected output:
# transferring model data
# using existing layer sha256:...
# writing manifest
# success
```

## ✅ Step 5: Test the Improved Model

### Automated Test
```bash
./ollama-models/test-cv-expert.sh
```

### Manual Test
```bash
ollama run gemma3:12b-cv-expert "Evaluate this CV and return JSON:

Name: Test Developer
Experience: 8 years Kotlin, Spring Boot, microservices on AWS
Skills: Kotlin, Spring Boot, Docker, Kubernetes, PostgreSQL
Roles: Senior Developer at FinTech company (2020-2024)
"
```

### Quality Checklist
- ✅ Returns valid JSON
- ✅ Scores seem reasonable (not all 70-80)
- ✅ Strengths are specific and detailed
- ✅ Improvements are actionable
- ✅ Summary captures key points
- ✅ Better than base model (`gemma3:12b`)

## 🔄 Step 6: Use in Your Application

The application is already configured to use `gemma3:12b-cv-expert`.

```bash
# Rebuild and restart
mvn clean package -DskipTests
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local

# Test CV scoring
curl -X POST http://localhost:8080/api/cv-score/{candidateId}/run

# View all scores
curl http://localhost:8080/api/cv-score/all | jq
```

## 📊 Step 7: Monitor and Iterate

### Weekly Review
1. Check CV scores for accuracy
2. Note any scores that seem too high/low
3. Collect 1-2 more good examples
4. Add to Modelfile
5. Recreate model

### Monthly Refinement
1. Review score distribution (should have variety, not all clustered)
2. Add 5 more diverse examples from production use
3. Adjust system prompt if needed
4. Test quality improvement

### Path to Full Fine-Tuning (Future)
Once you have 50-100 **manually verified** scores from production use:
1. Export scored data to JSONL format
2. Fine-tune using Unsloth or similar (requires GPU)
3. Convert to Ollama format
4. Deploy as `gemma3:12b-cv-expert-finetuned`

## 📁 Files Reference

| File | Purpose | Status |
|------|---------|--------|
| `data/` | Original CV PDFs and DOCX | ✅ 13 files |
| `training-cvs.txt` | Extracted full text | ✅ Created |
| `Modelfile-cv-examples.txt` | Templates for scoring | ✅ Created |
| `SCORING-GUIDE.md` | Scoring criteria and examples | ✅ Created |
| `Modelfile-gemma3-cv-expert` | Main model definition | ⏭️ To update with your scores |
| `test-cv-expert.sh` | Test script | ✅ Exists |

## 🎯 Summary: What You Need to Do

1. **Open** `Modelfile-cv-examples.txt`
2. **Pick 5 CVs** (diverse experience levels)
3. **Score each one** using `SCORING-GUIDE.md` as reference
4. **Copy scored examples** into `Modelfile-gemma3-cv-expert`
5. **Run** `ollama create gemma3:12b-cv-expert -f ollama-models/Modelfile-gemma3-cv-expert --force`
6. **Test** with `./ollama-models/test-cv-expert.sh`
7. **Use** in your application

**Time estimate**: 1-2 hours for 5 examples

## 💡 Pro Tips

- **Start small**: 5 well-scored examples > 20 rushed ones
- **Be specific**: Reference actual technologies and years from CVs
- **Vary scores**: Don't make everyone 75-85
- **Balance feedback**: Even great CVs should have improvements
- **Test often**: Recreate and test after adding each batch of examples
- **Iterate**: This is not a one-time task - keep improving as you use the model

## 🆘 Troubleshooting

### Model creation fails
```bash
# Ensure base model exists
ollama pull gemma3:12b

# Try with verbose
ollama create gemma3:12b-cv-expert -f ollama-models/Modelfile-gemma3-cv-expert --force --verbose
```

### Scores seem off
- Review your few-shot examples
- Lower temperature in Modelfile (try 0.1-0.2)
- Add more diverse examples
- Check JSON format consistency

### Python packages missing
```bash
cd ollama-models
source venv/bin/activate
pip install PyPDF2 python-docx
```
