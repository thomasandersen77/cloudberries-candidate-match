# Training Gemma3 for CV Evaluation

This guide explains how to create a CV-specialized version of Gemma3 using Ollama's customization features.

## Overview

We can customize Gemma3 in three ways:

1. **Modelfile Customization** (Easiest - Done here)
   - Custom system prompts
   - Parameter tuning
   - Few-shot examples
   - ✅ No GPU required
   - ✅ Fast to create (seconds)

2. **Fine-tuning with Training Data** (Advanced)
   - Requires training dataset of CV evaluations
   - Needs GPU for efficient training
   - More permanent learning
   - Takes hours/days

3. **Hybrid Approach** (Recommended)
   - Start with Modelfile customization
   - Collect evaluation data from usage
   - Fine-tune later with real data

## Quick Start: Create CV-Expert Model

### Step 1: Create the Model

```bash
# From project root
cd /Users/tandersen/git/cloudberries-candidate-match

# Create the CV-expert model from Modelfile
ollama create gemma3:12b-cv-expert -f ollama-models/Modelfile-gemma3-cv-expert

# Expected output:
# transferring model data
# using existing layer sha256:...
# writing manifest
# success
```

### Step 2: Test the Model

```bash
# Test with a sample CV evaluation
ollama run gemma3:12b-cv-expert "Evaluate this CV and provide a score as JSON:

Name: Test Developer
Experience:
- Senior Kotlin Developer at TechCorp (2020-2024): Spring Boot, microservices, Kubernetes
- Java Developer at StartupX (2018-2020): Backend APIs, PostgreSQL

Skills: Kotlin, Java, Spring Boot, Docker, Kubernetes, PostgreSQL"

# Should return structured JSON with score, summary, strengths, improvements
```

### Optional: Export full Flowcase CV catalog for local training prep

Use the manual integration test to pull all CVs from Flowcase and write them to the local data catalog used for model-improvement workflows.

Required environment variables in your shell:
- `FLOWCASE_BASE_URL` (must start with `http://` or `https://`)
- `FLOWCASE_API_KEY`

```bash
# From repository root
mvn -pl candidate-match -Dflowcase.manual=true \
  -Dtest=FlowcaseHttpClientIntegrationTest#exportAllConsultantsToOllamaTrainingCatalog test
```

Generated output:
- `ollama-models/data/flowcase-cv-catalog/json/*.json` (raw CV JSON per consultant)
- `ollama-models/data/flowcase-cv-catalog/text/*.txt` (flattened CV text per consultant)
- `ollama-models/data/flowcase-cv-catalog/training/cv-evaluation-prompts.jsonl`
- `ollama-models/data/flowcase-cv-catalog/training/cv-evaluation-label-template.jsonl`
- `ollama-models/data/flowcase-cv-catalog/training/Modelfile-flowcase-cv-examples.txt`
- `ollama-models/data/flowcase-cv-catalog/training/export-manifest.json`

This gives you a repeatable local dataset export for iterating on your `gemma3:12b-cv-expert` model.

### Step 3: Update Application Configuration

The configuration has already been updated to use `gemma3:12b-cv-expert`.

If the model doesn't exist yet, it will fail. Create it first, or temporarily use:
```yaml
ollama:
  model: gemma3:12b  # Fallback to base model
```

## What the Modelfile Does

### 1. System Prompt
Defines the model's role as a CV expert with:
- Software development expertise
- Nordic labor market knowledge
- Structured evaluation approach

### 2. Parameter Tuning

| Parameter | Value | Purpose |
|-----------|-------|---------|
| `temperature` | 0.3 | Consistent, factual responses (vs creative) |
| `top_p` | 0.9 | Focused sampling |
| `top_k` | 40 | Moderate token diversity |
| `num_ctx` | 8192 | Large context for full CVs |
| `repeat_penalty` | 1.1 | Avoid repetitive text |

### 3. Few-Shot Examples
Provides 2 example CV evaluations:
- High-quality senior candidate (score: 85)
- Junior candidate with gaps (score: 35)

This teaches the model:
- JSON output format
- Scoring scale (0-100)
- Balance between strengths and improvements
- Level of detail expected

## Advanced: Fine-Tuning with Your Data

### Prerequisites
- GPU with CUDA (or Apple Silicon with MLX)
- Python 3.10+
- Training dataset

### Step 1: Collect Training Data

Create a JSONL file with your CV evaluations:

```jsonl
{"prompt": "Evaluate this CV:\n[CV content]", "response": "{\"scorePercentage\": 85, \"summary\": \"...\", ...}"}
{"prompt": "Score this candidate:\n[CV content]", "response": "{\"scorePercentage\": 70, \"summary\": \"...\", ...}"}
```

Example script to export from your database:

```kotlin
// Add to candidate-match module
@Service
class CvTrainingDataExporter(
    private val consultantRepository: ConsultantRepository,
    private val cvScoreRepository: CvScoreRepository
) {
    fun exportTrainingData(outputFile: String) {
        val consultants = consultantRepository.findAll()
        
        File(outputFile).bufferedWriter().use { writer ->
            consultants.forEach { consultant ->
                val score = cvScoreRepository.findByCandidateUserId(consultant.userId)
                if (score != null) {
                    val prompt = "Evaluate this CV:\n${consultant.resumeData}"
                    val response = """
                        {
                          "scorePercentage": ${score.scorePercent},
                          "summary": "${score.summary}",
                          "strengths": ${score.strengths},
                          "improvements": ${score.potentialImprovements}
                        }
                    """.trimIndent()
                    
                    writer.write("""{"prompt": "$prompt", "response": "$response"}""")
                    writer.newLine()
                }
            }
        }
    }
}
```

### Step 2: Fine-Tune with Ollama (Future Feature)

Ollama is adding fine-tuning support. When available:

```bash
# Fine-tune the model (future Ollama feature)
ollama train gemma3:12b \
  --dataset cv-evaluations.jsonl \
  --output gemma3:12b-cv-expert-finetuned \
  --epochs 3 \
  --learning-rate 0.0001
```

### Step 3: Alternative - Use Unsloth/LLaMA-Factory

For now, use external tools:

```bash
# Install Unsloth for efficient fine-tuning
pip install unsloth

# Fine-tune script (example)
python fine_tune_gemma.py \
  --base_model google/gemma-3-12b \
  --dataset cv-evaluations.jsonl \
  --output_dir ./gemma3-cv-finetuned

# Convert to GGUF and import to Ollama
ollama create gemma3:12b-cv-finetuned -f Modelfile-finetuned
```

## Model Comparison

### Base Gemma3:12b
```bash
ollama run gemma3:12b "Evaluate this CV: [content]"
```
- Generic responses
- May miss CV-specific patterns
- Inconsistent JSON format
- Broader knowledge, less specialized

### CV-Expert (Modelfile)
```bash
ollama run gemma3:12b-cv-expert "Evaluate this CV: [content]"
```
- ✅ Consistent JSON output
- ✅ CV-specific evaluation criteria
- ✅ Balanced strengths/improvements
- ✅ Nordic market awareness
- ⚠️ Limited to prompt engineering

### CV-Expert (Fine-tuned)
```bash
ollama run gemma3:12b-cv-expert-finetuned "Evaluate this CV: [content]"
```
- ✅ All Modelfile benefits
- ✅ Learns from your actual evaluations
- ✅ Better pattern recognition
- ✅ More aligned with your scoring standards
- ⚠️ Requires training data and GPU

## Testing & Validation

### Create Test Script

```bash
#!/bin/bash
# test-cv-expert.sh

MODEL="gemma3:12b-cv-expert"

echo "Testing CV Expert Model..."
echo "=========================="

# Test 1: Senior candidate
echo "\n1. Senior Candidate Test:"
ollama run $MODEL "Evaluate this CV as JSON:

Name: Senior Dev
Experience:
- Tech Lead at BigCorp (2019-2024): Kotlin, microservices, led 8 engineers
- Senior Developer at Consulting (2015-2019): Java, Spring, AWS
Skills: Kotlin, Java, Spring Boot, Kubernetes, AWS, PostgreSQL, React"

# Test 2: Junior candidate
echo "\n2. Junior Candidate Test:"
ollama run $MODEL "Score this CV:

Name: Junior Dev
Experience:
- Junior Developer at Startup (2023-2024): Python, Flask
- Intern (2022): JavaScript, HTML
Skills: Python, JavaScript, Git"

# Test 3: Format consistency
echo "\n3. Format Test (should return valid JSON):"
ollama run $MODEL "Evaluate and score this CV: [minimal CV]" | jq .
```

### Run Tests

```bash
chmod +x ollama-models/test-cv-expert.sh
./ollama-models/test-cv-expert.sh
```

## Integration with Application

The application is already configured to use the CV-expert model:

```yaml
# application-local.yaml
ollama:
  model: gemma3:12b-cv-expert
  readTimeoutSeconds: 120  # Gemma3 is efficient
```

### Verify Integration

```bash
# Start application
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local

# Test CV scoring endpoint
curl -X POST http://localhost:8080/api/cv-score/67e27aeb4d749c0040dd0206/run

# Should use gemma3:12b-cv-expert for evaluation
```

## Iterative Improvement

### 1. Monitor Quality
- Review CV scores for accuracy
- Check JSON format consistency
- Validate strengths/improvements relevance

### 2. Adjust Modelfile
Edit `Modelfile-gemma3-cv-expert`:
- Refine system prompt
- Adjust parameters (e.g., temperature)
- Add more few-shot examples

Recreate:
```bash
ollama create gemma3:12b-cv-expert -f ollama-models/Modelfile-gemma3-cv-expert --force
```

### 3. Collect Data
- Save good evaluations to training dataset
- Include edge cases (very senior, very junior, career changers)
- Aim for 100+ diverse examples

### 4. Fine-Tune (When Ready)
- Use collected data for fine-tuning
- Validate on held-out test set
- Deploy fine-tuned model

## Tips for Better Results

### System Prompt Engineering
- Be specific about evaluation criteria
- Provide examples of good vs bad CVs
- Define scoring rubric clearly

### Parameter Tuning
- **Lower temperature (0.1-0.3)**: More deterministic, factual
- **Higher temperature (0.7-1.0)**: More creative, varied
- **Larger num_ctx**: Better for long CVs

### Few-Shot Examples
- Include diverse examples (senior, junior, different domains)
- Show desired output format
- Demonstrate edge cases

## Troubleshooting

### Model Creation Fails
```bash
# Ensure base model exists
ollama pull gemma3:12b

# Recreate with verbose logging
ollama create gemma3:12b-cv-expert -f ollama-models/Modelfile-gemma3-cv-expert --verbose
```

### Application Can't Find Model
```bash
# List models
ollama list | grep cv-expert

# If missing, create it
ollama create gemma3:12b-cv-expert -f ollama-models/Modelfile-gemma3-cv-expert

# Or temporarily use base model
# Update application-local.yaml: model: gemma3:12b
```

### Inconsistent JSON Output
- Add more few-shot examples
- Lower temperature (0.1-0.2)
- Add JSON schema validation in application

### Scores Too High/Low
- Adjust few-shot examples to show realistic scoring
- Refine system prompt with clearer criteria
- Consider fine-tuning with real evaluation data

## Next Steps

1. ✅ Create CV-expert model
2. ✅ Test with sample CVs
3. ✅ Integrate with application
4. ⏭️ Monitor evaluation quality
5. ⏭️ Collect training data from production use
6. ⏭️ Fine-tune when you have 100+ examples
7. ⏭️ Iterate on model improvements

## Resources

- [Ollama Modelfile Documentation](https://github.com/ollama/ollama/blob/main/docs/modelfile.md)
- [Gemma 3 Model Card](https://ai.google.dev/gemma/docs)
- [Unsloth Fine-tuning](https://github.com/unslothai/unsloth)
- [LLaMA-Factory](https://github.com/hiyouga/LLaMA-Factory)

## Example: Norwegian CV Context

Add to system prompt for better Nordic market understanding:

```
Additional context for Norwegian/Nordic market:
- "Fagbrev" = Vocational certificate (highly valued)
- "Høgskole/Universitet" = University degree
- Typical career: Trainee → Junior → Senior → Lead → Architect/Manager
- Consultant roles are common and valued
- Strong emphasis on work-life balance
- Public sector experience (Nav, Helsenorge, etc.) is prestigious
```

Update Modelfile and recreate model with this context.
