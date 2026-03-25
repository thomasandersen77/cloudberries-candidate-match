# TRAINING-MODEL-PLAYBOOK.md

# Objective

Inspect this repository and redesign the **local model training/build/evaluation workflow** for candidate matching and CV scoring.

Focus **only** on the training/model side.

Do **not** spend time on frontend/backend integration unless it is strictly required to support training, benchmarking, or local model execution.

The goal is to make the model workflow:

- more reliable
- more measurable
- more deterministic
- more robust for incomplete CVs
- better for local laptop execution
- easier to compare model variants
- easier to improve over time without guesswork

We already have working backend/frontend flows for:
- scoring one candidate
- scoring all candidates

Do not redesign those flows unless the training/build scripts directly depend on them.

---

# Important constraints

- local-first only
- zero paid APIs
- laptop-friendly
- prefer simple and robust over fancy
- do not assume full fine-tuning is necessary
- do not assume the current pipeline is wrong; inspect first
- optimize for:
    1. data quality
    2. structured outputs
    3. benchmark quality
    4. few-shot quality
    5. model comparison
    6. only then any fine-tuning strategy

---

# Important operating mode

You must **inspect the repository first** and adapt your implementation to the actual codebase.

Do not assume:
- filenames
- folder layout
- languages
- current architecture
- model names
- script names
- Modelfile structure
- benchmark setup

Before changing code, find and summarize:

1. where CV source data is stored
2. where extracted CV text is stored
3. where training/build scripts live
4. where Modelfiles or model prompt templates live
5. where current model parameters are configured
6. where model evaluation/scoring scripts live
7. where current few-shot examples are selected
8. where current scoring labels come from
9. where JSON parsing and failure handling happen
10. where benchmark or regression test outputs are stored
11. whether there is already caching, resumability, or dataset splitting
12. whether there are already multiple model roles (fast vs expert)

You must start by producing:

- repository map
- current training/build pipeline summary
- current evaluation pipeline summary
- current few-shot/example selection summary
- current failure points
- concrete proposed redesign

Do not start coding until that summary is written.

---

# Problem statement

We currently have a local candidate-match / CV-expert setup.
It already works to some degree, but the training/build workflow needs to become much more disciplined.

Known likely issues to solve:

- malformed model outputs
- prose instead of valid JSON
- failures on incomplete CVs
- weak separation between:
    - dataset quality
    - prompt quality
    - model quality
- too much dependence on ad-hoc prompt tweaking
- too little measurable benchmarking
- few-shot selection likely based on convenience rather than coverage
- unclear use of all available CVs

The improved workflow must treat training as a **data + schema + benchmarking + prompt discipline** problem first, not just “swap models and hope”.

---

# Dataset policy

We have around 110 CVs.

Use **all** of them, but not in one bucket.

The correct strategy is:

## 1. Corpus set
Use all CVs, including incomplete and invalid ones.

Purpose:
- retrieval corpus
- parsing corpus
- candidate profile generation
- negative case coverage
- robustness testing

## 2. Gold set
Manually trusted set.

Purpose:
- real evaluation
- comparing prompts/models fairly
- regression testing

Target size:
- 30 to 50 CVs

Each gold item should have trusted labels such as:
- status
- total score
- score breakdown
- seniority
- shortlist tags
- quality class
- invalid/insufficient-data classification

## 3. Dev set
Purpose:
- prompt iteration
- schema iteration
- few-shot tuning
- model parameter tuning

Target size:
- 15 to 25 CVs

## 4. Frozen test set
Purpose:
- final comparison between model variants and prompt versions
- must never be used as few-shot examples

Target size:
- 15 to 20 CVs

## 5. Edge case set
Purpose:
- robustness

Must include:
- empty CV
- email-only CV
- name-only CV
- name + title only
- very short CV
- broken extraction
- repeated boilerplate
- garbled text
- duplicate or near-duplicate CVs

Important:
More data is **not automatically** better training.
For this system:
- better labels are better
- broader case coverage is better
- more reliable JSON is better
- stronger failure handling is better
- benchmark quality is better

---

# Training philosophy

Treat the current workflow as **local specialization**, not necessarily full fine-tuning.

The improvement order must be:

1. deterministic preprocessing
2. dataset cleanup and splitting
3. strict output schema
4. benchmark harness
5. few-shot example quality
6. parameter discipline
7. model comparison
8. only then consider fine-tuning

Do not jump straight to “train harder”.

---

# Primary deliverable

Implement or refactor a **training pipeline** that supports:

- ingest
- preprocessing
- classification
- profile extraction
- labeling
- few-shot selection
- Modelfile / prompt generation
- benchmarking
- model comparison
- reproducibility
- caching
- resumability

---

# Stage 0: repository inspection

Inspect the repository and identify the existing equivalents of:

- training scripts
- build scripts
- model files
- prompt files
- scoring scripts
- evaluation outputs
- example selection logic
- CV data directories
- benchmark directories
- config files
- cache directories

Produce a concise but complete summary:

## Output required before code changes
1. What I looked at & why
2. Current pipeline
3. Current weaknesses
4. Proposed improved pipeline
5. File-by-file patch plan

Only after this should you edit files.

---

# Stage 1: CV ingest and fingerprinting

Implement or improve a CV ingest step.

For every CV or CV text file:

- assign stable candidate id
- assign stable document id
- compute checksum/hash
- record file path
- record source type
- record extraction length
- record language when possible
- record if duplicate
- record if near-duplicate when possible
- record if clearly broken

Persist this metadata in a structured format.

Recommended persisted fields:

```json
{
  "candidateId": "string",
  "documentId": "string",
  "sourcePath": "string",
  "checksum": "string",
  "charCount": 0,
  "lineCount": 0,
  "language": "string",
  "isDuplicate": false,
  "isNearDuplicate": false,
  "ingestWarnings": ["string"]
}

Requirements:
- deterministic
- resumable
- incremental
- skip unchanged files unless forced

---

Stage 2: deterministic preprocessing and quality classification

Implement preprocessing before any model call.

This stage must detect:
- empty file
- extremely short file
- email-only content
- name-only content
- title-only content
- name + title only
- missing work history
- missing date ranges
- broken extraction
- repeated boilerplate
- non-CV content
- heavily corrupted text

This stage must produce:
- cleaned text
- warnings
- quality metrics
- classification

Allowed classes:
- ok
- insufficient_data
- invalid_cv

Persist preprocessing output.

Example shape:

{
  "candidateId": "string",
  "status": "ok | insufficient_data | invalid_cv",
  "cleanedText": "string",
  "qualitySignals": {
    "hasWorkHistory": true,
    "hasDates": true,
    "hasSkillsSection": true,
    "hasOnlyEmail": false,
    "hasOnlyName": false,
    "isVeryShort": false,
    "looksCorrupted": false
  },
  "warnings": ["string"]
}

Rules:
- deterministic rules first
- if preprocessing already proves the CV is invalid, downstream scoring may skip the LLM
- incomplete CVs are not pipeline failures; they are valid cases

---

Stage 3: candidate profile extraction

Implement or improve a pipeline that turns preprocessed CV text into a normalized candidate profile.

Use deterministic extraction first wherever possible.
Use LLM-based normalization only where deterministic logic is insufficient.

Each candidate profile should contain fields equivalent to:

{
  "candidateId": "string",
  "name": "string",
  "summary": "string",
  "totalYearsExperience": 0,
  "seniorityLevel": "junior | mid | senior | principal | unknown",
  "roles": ["Backend Developer", "Tech Lead"],
  "technologies": ["Java", "Kotlin", "Spring Boot", "AWS"],
  "technologySignals": {
    "java": 0,
    "kotlin": 0,
    "springBoot": 0,
    "aws": 0,
    "azure": 0,
    "docker": 0,
    "kubernetes": 0,
    "react": 0,
    "typescript": 0,
    "aiMl": 0
  },
  "domains": ["Public Sector", "Finance"],
  "leadershipExperience": true,
  "cloudExperience": true,
  "languageQuality": "high | medium | low",
  "dataQuality": "high | medium | low | invalid",
  "warnings": ["string"]
}

Requirements:
- versioned output
- cacheable
- deterministic where possible
- reusable in benchmarks
- not tied to one specific model

---

Stage 4: trusted labels and gold set

Add or improve a manual labeling workflow.

Create a simple, versioned label format for the gold set.

Each gold label file should support fields equivalent to:

{
  "candidateId": "string",
  "status": "ok | insufficient_data | invalid_cv",
  "scorePercentage": 0,
  "scoreBreakdown": {
    "structureAndProfessionalism": 0,
    "projectAndRoleDescriptions": 0,
    "technicalDepth": 0,
    "leadershipAndInitiative": 0,
    "aiAndModernTech": 0
  },
  "notes": ["string"],
  "tags": ["senior", "backend", "java", "kotlin", "public-sector"]
}

Requirements:
- human-editable
- version controlled
- easy to diff
- easy to compare against model outputs

---

Stage 5: few-shot selection redesign

Refactor the current few-shot selection logic.

Do not select few-shot examples only by score percentile or convenience.

Few-shot examples must be selected by coverage.

The selector must intentionally cover:
- strong senior backend
- strong fullstack
- strong leadership
- AI/ML signal
- public sector / enterprise
- Kotlin/Java-heavy
- average candidate
- weaker candidate
- high-substance but weakly structured CV
- good structure but limited technical depth
- invalid CV
- insufficient-data CV

Target size:
- 8 to 15 examples

Requirements:
- avoid duplicates
- avoid near-duplicates
- avoid uncertain labels
- include 1 to 2 negative/invalid examples
- keep total context size under control
- output a manifest explaining why each example was selected

Example manifest shape:

{
  "fewShotSetVersion": "string",
  "examples": [
    {
      "candidateId": "string",
      "reason": "strong senior backend Kotlin/Java coverage",
      "category": "strong_backend"
    }
  ]
}


---

Stage 6: strict schema-first scoring

Every scoring model invocation must be schema-first.

Do not accept “best effort prose”.
Do not accept “No JSON found”.
Do not accept “please provide the full CV”.

If the model is called for scoring, the response must conform to this schema:

{
  "status": "ok | insufficient_data | invalid_cv",
  "scorePercentage": 0,
  "summary": "string",
  "strengths": ["string"],
  "improvements": ["string"],
  "scoreBreakdown": {
    "structureAndProfessionalism": {
      "score": 0,
      "justification": "string"
    },
    "projectAndRoleDescriptions": {
      "score": 0,
      "justification": "string"
    },
    "technicalDepth": {
      "score": 0,
      "justification": "string"
    },
    "leadershipAndInitiative": {
      "score": 0,
      "justification": "string"
    },
    "aiAndModernTech": {
      "score": 0,
      "justification": "string"
    }
  },
  "warnings": ["string"]
}

Rules:
- no prose outside JSON
- no refusal
- no request for more data
- invalid or incomplete CVs must still return valid JSON
- if preprocessing already classifies the CV as invalid, allow a deterministic non-LLM response path
- if model output is invalid, perform at most one repair retry
- if still invalid, store failure artifact and mark benchmark failure

---

Stage 7: model roles

Support two logical model roles in the training/build system.

1. Expert scoring model

Purpose:
- scoring
- comparison
- structured evaluation

Characteristics:
- low temperature
- strict JSON
- higher context
- reliable and deterministic

2. Fast model

Purpose:
- lightweight auxiliary tasks if needed
- intent parsing
- small helper tasks during offline evaluation or search experiments

Characteristics:
- smaller and faster
- lower context
- stable, not verbose

Important:
Both roles must share:
- same domain vocabulary
- same skills dictionary
- same seniority logic
- same quality class definitions
- same scoring philosophy

Do not create two disconnected systems.

If the repo currently uses only one model role, preserve compatibility but make it possible to add the second cleanly.

---

Stage 8: parameter profiles

Create explicit inference profiles in config, not scattered inline.

Expert profile defaults

Use these as recommended starting points:
- temperature: 0.1 to 0.2
- top_k: 20 to 40
- top_p: 0.85 to 0.95
- repeat_penalty: 1.05 to 1.10
- num_ctx: 8192 to 16384
- seed: fixed in benchmark mode
- schema validation: required
- retries: 1 maximum
- keep_alive: enabled in service mode if supported

Fast profile defaults

Use these as recommended starting points:
- temperature: 0.2 to 0.4
- top_k: 30 to 60
- top_p: 0.90 to 0.97
- num_ctx: lower than expert when possible
- keep_alive: enabled where appropriate

Implement these as named profiles.

Do not overfit parameters without benchmarks.

---

Stage 9: inference parameter documentation

Generate documentation that explains, in practical terms:
- temperature
- top_k
- top_p
- min_p if supported
- num_ctx
- repeat_penalty
- seed
- stop sequences
- num_predict
- keep_alive

For each parameter, explain:
- what it does
- how it affects CV scoring
- how it affects fast tasks
- recommended default here
- what breaks when it is too high
- what breaks when it is too low

This documentation must be practical, not theoretical.

---

Stage 10: Modelfile / prompt generation discipline

Inspect the existing model build flow and improve it.

Requirements:
- version prompt inputs
- version schema inputs
- version few-shot manifests
- version model profile settings
- make generated Modelfiles reproducible
- separate:
  - prompt templates
  - example manifests
  - schema files
  - parameter profiles
- avoid embedding hard-coded example blobs directly in scripts where possible

If the current pipeline generates Modelfiles from selected CVs:
- preserve that behavior
- but make selection, prompt generation, and configuration easier to audit

---

Stage 11: benchmark harness

Implement or improve a benchmark runner that compares prompt/model variants.

It must measure:

Reliability
- JSON valid rate
- schema pass rate
- refusal rate
- invalid-input handling rate

Quality
- agreement with gold labels
- class accuracy for ok, insufficient_data, invalid_cv
- score correlation or score deviation vs gold labels
- stability across repeated runs with fixed seed
- behavior on negative examples

Performance
- latency per CV
- total runtime
- cache hit rate
- resumability
- number of failures
- output size

Persist benchmark results to disk in structured format.

Example benchmark record:

{
  "runId": "string",
  "model": "string",
  "promptVersion": "string",
  "schemaVersion": "string",
  "profile": "expert",
  "dataset": "gold",
  "jsonValidRate": 0.0,
  "schemaPassRate": 0.0,
  "refusalRate": 0.0,
  "meanLatencyMs": 0,
  "failures": 0
}


---

Stage 12: model comparison runner

Implement a comparison workflow for multiple local model variants.

The comparison runner must make it easy to compare combinations like:
- current expert model
- improved expert model
- same model with different prompt versions
- same model with different few-shot manifests
- alternative local model family if configured

The output should include:
- summary table
- regression warnings
- recommendation for current best model configuration

Do not assume any specific model family.
Use whatever the repo currently supports and make it extensible.

---

Stage 13: failure artifact handling

Improve failure handling so errors become useful learning signals.

For failed runs in benchmark/dev mode, persist:
- candidate id
- prompt version
- schema version
- model name
- profile name
- latency
- raw output
- failure reason
- preprocessing classification

Requirements:
- benchmark/dev mode only
- avoid production leakage of sensitive data
- easy to inspect later

This is essential for learning from malformed outputs.

---

Stage 14: caching and incremental efficiency

Make the workflow efficient.

Implement or improve:
- caching of preprocessing outputs
- caching of profile extraction outputs
- caching of scoring outputs
- cache keys based on:
  - input content hash
  - model version
  - prompt version
  - schema version
  - parameter profile
- resumable runs
- incremental rebuilds
- skip unchanged CVs unless forced
- optional safe parallelization where supported

The pipeline must support long runs without losing progress.

---

Stage 15: repo outputs to add or align

Adapt these to the real repo structure after inspection.

Create or update equivalents of:

Documentation
- docs/ai/training-pipeline.md
- docs/ai/dataset-strategy.md
- docs/ai/benchmarking.md
- docs/ai/inference-parameters.md
- docs/ai/model-strategy.md

Schemas
- schemas/cv-score.schema.json
- schemas/candidate-profile.schema.json
- schemas/training-run.schema.json

Prompt-related files
- prompt template(s) for scoring
- prompt template(s) for repair
- prompt template(s) for profile extraction if needed

Scripts or modules
- ingest
- preprocess
- profile extraction
- label utilities
- few-shot selection
- Modelfile generation
- benchmark runner
- comparison runner

Data directories
- corpus metadata
- profiles
- gold labels
- dev labels
- test labels
- edge cases
- benchmarks
- failure artifacts

Do not force this exact structure if the repo already has a better convention. Adapt cleanly.

---

Stage 16: tests

Add or improve tests for:

Preprocessing
- empty CV
- email-only CV
- name-only CV
- name + title only
- very short CV
- broken extraction
- duplicate detection where feasible

Schema handling
- valid JSON response
- malformed JSON response
- repair retry
- invalid output after retry

Few-shot selection
- coverage selection
- duplicate avoidance
- negative example inclusion

Benchmarking
- result persistence
- regression comparison
- summary generation

Classification
- ok
- insufficient_data
- invalid_cv

Stability
- deterministic behavior with fixed seed where applicable

Tests should be mapped to acceptance criteria.

---

Acceptance criteria

The work is complete only when all of the following are true:
1. The repository has a clearly documented training/build/evaluation workflow.
2. All 110 CVs can be used in a disciplined multi-set strategy.
3. Invalid and incomplete CVs are handled as first-class cases.
4. Few-shot examples are selected by coverage, not convenience alone.
5. Scoring is schema-first and benchmarked.
6. Model outputs can be compared reproducibly.
7. Failures are inspectable instead of silently discarded.
8. Incremental reruns are supported.
9. Parameter profiles are explicit and documented.
10. The workflow remains local-first and laptop-friendly.

---

Required output format

Produce work in this order:

1. What I looked at & why

List the relevant repo paths, files, and areas you inspected and why each matters.

2. Findings

Describe:
- current training/build flow
- current benchmark flow
- current example selection flow
- weaknesses
- risks
- likely causes of failure

3. Recommendations (ranked)

For each recommendation:
- change
- benefit
- reason/principle

4. Design sketch

Show:
- training pipeline stages
- responsibilities
- data flow
- artifacts produced at each stage

5. Code changes (diff-ready)

Provide:
- file-by-file patch plan
- patch-style diffs where feasible
- full contents for new files

6. Tests

List:
- unit tests
- integration tests
- benchmark tests
- acceptance mapping

7. Benchmark commands

Show:
- local commands
- expected outputs
- where results are stored

8. Migration notes

Explain:
- what changed
- how to run the new workflow
- how to preserve compatibility with current model builds

9. Recommended commit sequence

Use Conventional Commits.

---

Final instruction

Do not spend time on API contracts, frontend wiring, or application UI.

Focus entirely on:
- training/build scripts
- dataset strategy
- preprocessing
- schema discipline
- few-shot selection
- benchmarking
- model comparison
- reproducibility
- efficiency

The priority is not to make the model “sound smart”.
The priority is to make the training workflow measurable, robust, and improvable.

