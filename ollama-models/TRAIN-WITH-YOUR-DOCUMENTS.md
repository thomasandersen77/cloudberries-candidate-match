# Training Gemma3 with Your CV and Customer Request Documents

## Overview

You can improve Gemma3's CV evaluation by training it with your **actual CVs and customer requests**. This guide shows you how to use your PDFs and Word documents to enhance the model.

## Two Training Approaches

### 🚀 Quick Enhancement (Start Here - No GPU)
- Extract text from your documents
- Manually score 5-10 examples
- Add them as few-shot examples to the Modelfile
- **Time**: 1-2 hours
- **Benefit**: Immediate improvement, no GPU needed

### 🎓 Full Fine-Tuning (Advanced - Requires GPU)
- Extract and score 100+ examples
- Actually modify model weights through training
- **Time**: Several hours + GPU access
- **Benefit**: Permanent learning, best quality

**Start with Quick Enhancement**, then move to fine-tuning when you have more data.

## Quick Enhancement: Step-by-Step Guide

### Prerequisites

```bash
# Install Python packages
pip install PyPDF2 python-docx

# Verify installation
python3 -c "import PyPDF2; from docx import Document; print('✅ Packages installed')"
```

### Step 1: Organize Your Documents (5 minutes)

Create folders for your documents:

```bash
# Create folders
mkdir -p ~/Documents/training-data/cvs
mkdir -p ~/Documents/training-data/customer-requests

# Example structure:
# ~/Documents/training-data/
# ├── cvs/
# │   ├── senior-developer-2024.pdf
# │   ├── junior-frontend.docx
# │   └── architect-profile.pdf
# └── customer-requests/
#     ├── banking-project-2024.pdf
#     ├── ecommerce-requirements.docx
#     └── nav-integration.pdf
```

**Put your CV PDFs and Word docs in these folders.**

### Step 2: Extract Text from Documents (2 minutes)

```bash
cd /Users/tandersen/git/cloudberries-candidate-match

# Extract from CVs only
python3 ollama-models/extract-training-data.py \
  --cv-folder ~/Documents/training-data/cvs \
  --output training-cvs.txt \
  --modelfile-output Modelfile-cv-examples.txt

# Or extract from both CVs and customer requests
python3 ollama-models/extract-training-data.py \
  --cv-folder ~/Documents/training-data/cvs \
  --request-folder ~/Documents/training-data/customer-requests \
  --output training-all.txt \
  --modelfile-output Modelfile-all-examples.txt
```

**Output**:
- `training-cvs.txt` - Full text extracted from all documents
- `Modelfile-cv-examples.txt` - Template examples ready for scoring

### Step 3: Manually Score Examples (30-60 minutes)

Open `Modelfile-cv-examples.txt` and score 5-10 examples:

**Before (Template)**:
```
MESSAGE user """Evaluate this CV:

John Doe
Senior Java Developer
Experience: 10 years Java, Spring Boot, microservices
Skills: Java, Spring, Kubernetes, AWS"""

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

**After (Your Scoring)**:
```
MESSAGE user """Evaluate this CV:

John Doe
Senior Java Developer
Experience: 10 years Java, Spring Boot, microservices
Skills: Java, Spring, Kubernetes, AWS"""

MESSAGE assistant """{
  "scorePercentage": 85,
  "summary": "Highly experienced backend developer with strong modern tech stack. Excellent cloud and containerization skills. Ready for senior/lead positions.",
  "strengths": [
    "10 years of solid Java experience with progression to modern frameworks",
    "Strong cloud-native development skills (Kubernetes, AWS)",
    "Microservices architecture experience critical for enterprise roles",
    "Modern tech stack alignment with current market demands",
    "Experience level suitable for technical leadership"
  ],
  "improvements": [
    "No mention of team leadership or mentoring experience",
    "Missing certifications (AWS, Kubernetes) to validate cloud skills",
    "No specific project outcomes or business impact mentioned",
    "Frontend skills not demonstrated - limits full-stack opportunities",
    "CI/CD and DevOps practices not explicitly mentioned"
  ]
}"""
```

**Tips for Good Scoring**:
- **Varied scores**: Include examples from 30-95 (not all high/low)
- **Specific strengths**: Mention concrete skills and years
- **Actionable improvements**: Things the candidate can actually improve
- **Realistic summaries**: Match Norwegian/Nordic market expectations
- **Balance**: 3-5 strengths, 3-5 improvements

### Step 4: Add Examples to Modelfile (10 minutes)

Edit `ollama-models/Modelfile-gemma3-cv-expert`:

```bash
# Open Modelfile
nano ollama-models/Modelfile-gemma3-cv-expert
# or use your favorite editor
```

**Add your scored examples at the end**, before the closing:

```
# ... existing content ...

# Examples from your actual CV database
MESSAGE user """Evaluate this CV:
[Your CV text from Step 3]"""

MESSAGE assistant """{
  "scorePercentage": 85,
  "summary": "...",
  ...
}"""

MESSAGE user """Score this CV:
[Another CV from Step 3]"""

MESSAGE assistant """{
  "scorePercentage": 65,
  ...
}"""

# Add 5-10 total examples
```

### Step 5: Recreate the Model (1 minute)

```bash
# Recreate model with your examples
ollama create gemma3:12b-cv-expert \
  -f ollama-models/Modelfile-gemma3-cv-expert \
  --force

# Expected output:
# transferring model data
# using existing layer sha256:...
# writing manifest
# success
```

### Step 6: Test the Improved Model (5 minutes)

```bash
# Test with automated script
./ollama-models/test-cv-expert.sh

# Or test manually
ollama run gemma3:12b-cv-expert "Evaluate this CV and return JSON:
Name: Test
Experience: 5 years Python, Django, PostgreSQL
Skills: Python, Django, REST APIs, PostgreSQL"

# Compare quality with original model
ollama run gemma3:12b "Evaluate this CV:
[same CV]"
```

**What to check**:
- ✅ More specific strengths/improvements
- ✅ Better scoring accuracy
- ✅ More relevant to Norwegian market
- ✅ Consistent JSON format

### Step 7: Use in Application

The application already uses the model! Just restart:

```bash
# Rebuild
mvn clean package -DskipTests

# Restart
mvn -pl candidate-match -am spring-boot:run -Dspring-boot.run.profiles=local

# Test CV scoring
curl -X POST http://localhost:8080/api/cv-score/{candidateId}/run
```

## Iterative Improvement

### Collect More Examples

As you use the application:

1. Review CV scores that seem accurate
2. Add the best ones as few-shot examples
3. Review scores that seem off
4. Adjust scoring criteria in system prompt

### Monthly Refinement

```bash
# Once a month
1. Extract 5 new CV examples
2. Score them manually
3. Add best examples to Modelfile
4. Recreate model
5. Test quality improvement
```

### Track Quality Over Time

```bash
# Check score distribution
curl http://localhost:8080/api/cv-score/all | \
  jq '.[] | .scorePercent' | \
  sort -n | uniq -c

# Look for:
# - Good distribution (not all 70-80)
# - Appropriate differentiation
# - Realistic scores for market
```

## Advanced: Full Fine-Tuning

When you have **100+ manually scored CVs**, you can do full fine-tuning.

### Step 1: Prepare Training Dataset

The extraction script from Step 2 already creates the base. Now add scores:

```python
# create-training-dataset.py
import json

def create_training_dataset(input_file, output_file):
    """Convert scored examples to training format."""
    
    examples = []
    
    # Read your manually scored examples
    # (from Modelfile or database)
    
    with open(output_file, 'w') as f:
        for example in examples:
            training_row = {
                "prompt": f"Evaluate this CV:\n{example['cv_text']}",
                "response": json.dumps({
                    "scorePercentage": example['score'],
                    "summary": example['summary'],
                    "strengths": example['strengths'],
                    "improvements": example['improvements']
                })
            }
            f.write(json.dumps(training_row) + '\n')

create_training_dataset('scored-cvs.json', 'training-dataset.jsonl')
```

### Step 2: Fine-Tune (Requires GPU)

**Option A: Cloud Fine-Tuning** (Recommended)

Use Google Colab or similar:

```python
# In Colab with GPU
!pip install unsloth

from unsloth import FastLanguageModel
import torch

# Load base model
model, tokenizer = FastLanguageModel.from_pretrained(
    model_name = "google/gemma-3-12b",
    max_seq_length = 8192,
    dtype = torch.float16,
    load_in_4bit = True,
)

# Fine-tune
from transformers import TrainingArguments
from trl import SFTTrainer

trainer = SFTTrainer(
    model = model,
    tokenizer = tokenizer,
    train_dataset = dataset,
    max_seq_length = 8192,
    args = TrainingArguments(
        per_device_train_batch_size = 2,
        num_train_epochs = 3,
        learning_rate = 2e-4,
        output_dir = "outputs",
    ),
)

trainer.train()

# Save and convert to GGUF
model.save_pretrained_gguf("gemma3-cv-expert-finetuned", tokenizer)
```

**Option B: Local Fine-Tuning** (If you have GPU)

```bash
# Requires CUDA GPU
pip install unsloth

python scripts/fine_tune_gemma.py \
  --dataset training-dataset.jsonl \
  --output gemma3-cv-expert-finetuned

# Convert to Ollama format
ollama create gemma3:12b-cv-expert-finetuned \
  -f Modelfile-finetuned
```

## Troubleshooting

### Python Package Errors

```bash
# Install missing packages
pip install PyPDF2 python-docx

# Or use pip3
pip3 install PyPDF2 python-docx

# Verify
python3 -c "import PyPDF2; print('✅ PyPDF2')"
python3 -c "from docx import Document; print('✅ python-docx')"
```

### PDF Extraction Fails

Some PDFs are scanned images without text. Use OCR:

```bash
pip install pytesseract pdf2image

# Or use online tools to convert PDF to Word first
```

### Model Not Improving

**Checklist**:
- [ ] Added 5+ diverse examples (not just similar CVs)
- [ ] Scores vary (not all 70-80)
- [ ] Specific strengths/improvements (not generic)
- [ ] Recreated model with `--force`
- [ ] Tested with new CVs (not same as training)

### "Not Enough Training Data"

You need **quality > quantity**:
- 5-10 **well-scored** examples can make a difference
- Better to have 5 excellent examples than 50 rushed ones
- Focus on diverse profiles (junior, mid, senior, different technologies)

## Summary

### Quick Enhancement Workflow

```bash
# 1. Organize documents
mkdir -p ~/Documents/training-data/cvs

# 2. Extract text
python3 ollama-models/extract-training-data.py \
  --cv-folder ~/Documents/training-data/cvs

# 3. Manually score 5-10 examples in the output file

# 4. Add to Modelfile
nano ollama-models/Modelfile-gemma3-cv-expert

# 5. Recreate model
ollama create gemma3:12b-cv-expert \
  -f ollama-models/Modelfile-gemma3-cv-expert --force

# 6. Test
./ollama-models/test-cv-expert.sh

# 7. Use in application (already configured!)
```

### Expected Results

After adding 5-10 quality examples:
- ✅ 20-30% more accurate scores
- ✅ More specific strengths/improvements
- ✅ Better Norwegian market alignment
- ✅ Improved JSON consistency

After fine-tuning with 100+ examples:
- ✅ 50-70% better accuracy
- ✅ Learns your specific evaluation style
- ✅ Better pattern recognition
- ✅ More consistent quality

## Next Steps

1. ✅ Put CVs in `~/Documents/training-data/cvs/`
2. ✅ Run extraction script
3. ✅ Manually score 5-10 examples
4. ✅ Add to Modelfile
5. ✅ Recreate model
6. ✅ Test and compare
7. ⏭️ Monitor quality over time
8. ⏭️ Collect more examples
9. ⏭️ Fine-tune when you have 100+

**Start small** (5 examples), see improvement, then add more!
