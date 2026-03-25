---
name: query-interpretation-port-adapter
description: Implements the missing QueryInterpretationPort bean in ai-rag-service using AiContentGenerationPort, contracts DTOs, and dedicated prompts—without circular dependencies or imports from candidate-match. Use when Spring fails to wire QueryInterpretationPort, when AIQueryInterpretationService in candidate-match cannot find the bean, when adding query-interpretation adapters in ai-rag-service, or when the user mentions ACL, QueryInterpretationPort, or query interpretation prompts in this repo.
---

# Query interpretation port adapter (ai-rag-service)

## Goal

Provide a `@Service` implementation of `no.cloudberries.ai.port.QueryInterpretationPort` in **`ai-rag-service`** so **`candidate-match`** can inject it via component scanning of `no.cloudberries.ai`, without **`ai-rag-service` depending on `candidate-match`**.

## Module boundaries (do not violate)

| Module | Role |
|--------|------|
| `candidate-match` | Owns business logic, repos, JPA, transactions; may map contracts → core DTOs. **No large refactors in this task.** |
| `ai-platform-contracts` | Owns `QueryInterpretationPort`, `QueryInterpretation`, `StructuredCriteria`, `ConfidenceScores`, `SearchMode`. **Read contracts only** unless the user explicitly extends them. |
| `ai-rag-service` | Owns the adapter, prompt template, JSON parsing/fallback, and calls via `AiContentGenerationPort`. **Must not import `no.cloudberries.candidatematch.*`.** |

## Read-only references (understand behavior)

- **Contracts**: `ai-platform-contracts/src/main/kotlin/no/cloudberries/ai/port/QueryInterpretationPort.kt`, `.../dto/QueryInterpretation.kt`
- **Source for prompt shape / parsing / robustness** (do not move business logic here): `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/service/ai/AIQueryInterpretationService.kt`

## Implementation scope (edit here)

Under **`ai-rag-service`** only:

- `src/main/kotlin/no/cloudberries/ai/infrastructure/ai/` — adapter
- `src/main/kotlin/no/cloudberries/ai/templates/` — prompt template + params
- Small config/wiring changes only if required for beans

## Deliverables

### 1. Adapter class

Create:

`ai-rag-service/src/main/kotlin/no/cloudberries/ai/infrastructure/ai/QueryInterpretationAdapter.kt`

- `@Service`, implements `QueryInterpretationPort`
- Constructor-inject `AiContentGenerationPort`, `ObjectMapper` (and logger)
- Allowed imports: `no.cloudberries.ai.dto.*`, `no.cloudberries.ai.port.*`, internal templates/utilities in `ai-rag-service` only
- **Forbidden**: `no.cloudberries.candidatematch.*`, `CandidateMatchAIChatConfig`, any core DTO packages

### 2. Prompt template

Create e.g.:

`ai-rag-service/src/main/kotlin/no/cloudberries/ai/templates/QueryInterpretationPromptTemplate.kt`

Extend or add small param types next to existing `TemplateParams.kt` if cleaner.

**Expected JSON shape** (model output after fence stripping):

```json
{
  "route": "structured|semantic|hybrid|rag",
  "structured": {
    "skillsAll": [],
    "skillsAny": [],
    "roles": [],
    "minQualityScore": 85,
    "locations": [],
    "availability": null,
    "publicSector": true,
    "customersAny": [],
    "industries": []
  },
  "semanticText": "text",
  "consultantName": "name",
  "question": "question",
  "confidence": {
    "route": 0.87,
    "extraction": 0.92
  }
}
```

### 3. `interpretQuery` behavior

Parameters: `userText: String`, `forceMode: SearchMode?` (match the port signature exactly).

1. If `forceMode == null`: build full prompt from template.
2. Call `aiContentGenerationPort.generateContent(prompt)` (or the exact method on the port).
3. Strip markdown fences: remove leading `` ```json ``, trailing `` ``` ``, then `trim()`.
4. Parse JSON → `QueryInterpretation` (contracts DTO).
5. If `forceMode != null`: **override route** in the result to match `forceMode` (simplest compile-safe approach). Optionally still call AI for extraction; route must still reflect `forceMode`.
6. Clamp confidence values to **0.0..1.0**.
7. On parse failure: log clearly and throw a **controlled** exception (not silent fallback unless explicitly specified).

### 4. Tests (ai-rag-service)

Add unit tests for `QueryInterpretationAdapter` (MockK or project standard):

1. Valid JSON → correct `QueryInterpretation`
2. JSON wrapped in markdown fences → still parses
3. `forceMode` overrides route when AI returns a different route
4. Invalid JSON → expected exception type/message contract
5. *(Optional)* `@SpringBootTest` or slice test that the adapter is a bean

## Architecture principles (short)

- Adapter = **prompt → AI → parse → contracts DTO** only.
- Provider selection stays behind `AiContentGenerationPort`.
- No business rules that belong in `candidate-match` in this adapter.

## Stop condition (all must be true)

- [ ] `@Service` implements `QueryInterpretationPort` in `ai-rag-service`
- [ ] `candidate-match` can start and inject the port (scanning `no.cloudberries.ai`)
- [ ] No `ai-rag-service` → `candidate-match` dependency
- [ ] Prompt lives under `ai-rag-service` templates
- [ ] Contracts types used end-to-end
- [ ] Tests listed above exist
- [ ] No broad redesign outside this scope

## Response format (when executing this task)

1. What was read and why  
2. Findings  
3. Plan  
4. Patches / new files  
5. Tests added  
6. Why startup wiring is fixed  
