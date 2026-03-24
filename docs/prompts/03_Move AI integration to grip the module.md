











# Refactor Prompt: Move AI Integrations and Prompt Infrastructure out of `candidate-match` into `ai-rag-service`

You are working in a multi-module Maven **modular monolith** with these modules:

- `candidate-match` = core domain, repositories, transactional business logic, primary executable
- `ai-rag-service` = AI/RAG integration module
- `teknologi-barometer-service` = currently unclear responsibility, keep lightweight and align it with the same dependency style as `ai-rag-service`

## Goal

Refactor the solution so that:

1. `candidate-match` remains the **core module**
2. all **database repositories, JPA entities, and transactional core business logic remain in `candidate-match`**
3. all **AI provider integrations** move out of `candidate-match` and into `ai-rag-service`
4. all **hard-coded prompt templates / prompt builders** move out of `candidate-match` and into `ai-rag-service`
5. tests are moved accordingly:
   - business logic tests stay in `candidate-match`
   - AI adapter / prompt / provider wiring tests move to `ai-rag-service`
6. `teknologi-barometer-service` should, for now, be aligned structurally as a dependent satellite module in the same spirit as `ai-rag-service`
7. preserve behavior as much as possible and keep the build green

---

## Architectural rules you must follow

### 1. Core ownership
`candidate-match` is the core module and must keep ownership of:

- domain model
- application services / use cases
- JPA entities
- Spring Data repositories
- database migrations related to core data
- transaction boundaries
- matching/scoring business rules
- DTOs that belong to core API contracts

### 2. AI ownership
`ai-rag-service` must own:

- Gemini integration
- OpenAI integration
- Ollama integration
- Anthropic integration
- embedding provider implementations
- prompt templates
- prompt rendering/building
- RAG-specific orchestration
- AI-specific configuration
- AI integration tests
- AI adapter wiring

### 3. Do NOT create circular Maven dependencies
This is critical.

Because `candidate-match` is the core and currently the executable module, you must avoid a direct cyclic dependency between:

- `candidate-match`
- `ai-rag-service`

#### Required strategy
Use **ports and adapters**.

- define AI-facing ports/interfaces in the core side
- implement those ports in `ai-rag-service`

If a circular compile-time dependency would otherwise be introduced, create a **small shared contracts module** rather than forcing an illegal two-way dependency.

Preferred fallback module names if needed:

- `candidate-match-contracts`
- or `ai-platform-contracts`

Only introduce that module if truly needed to keep the dependency graph clean.

### 4. Same database, no ownership duplication
All modules share the same database instance, but only `candidate-match` owns repositories and persistence model for core data.

That means:

- no duplicated JPA entities in `ai-rag-service`
- no duplicated repositories in `ai-rag-service`
- `ai-rag-service` may consume core abstractions or DTO snapshots, but must not become a second persistence owner

### 5. Preserve transactional boundaries
State changes must remain orchestrated by `candidate-match`.

`ai-rag-service` may analyze/generate/embed, but must not become the owner of transactional business flows for candidates, project requests, or matching results.

---

## Analyze and refactor these concrete areas

### In `candidate-match`, identify and move out AI infrastructure such as:

- `infrastructure/integration/gemini/*`
- `infrastructure/integration/openai/*`
- `infrastructure/integration/ollama/*`
- `infrastructure/integration/anthropic/*`
- `infrastructure/integration/embedding/*`
- `infrastructure/integration/ai/AIContentGeneratorFactory.kt`

### Also move prompt-related code out of `candidate-match`, including:

- `templates/*`
- any hard-coded prompts in services such as:
  - `service/projectrequest/ProjectRequestAnalysisService.kt`
  - `service/matching/CandidateMatchingService.kt`
  - `service/ai/AIQueryInterpretationService.kt`
  - any scoring / analysis service containing inline prompt assembly

### Keep in `candidate-match`:

- repositories
- JPA entities
- domain model
- matching/scoring logic
- project request business logic
- controller contracts
- service orchestration that belongs to the core use cases

---

## Required target design

### Core side (`candidate-match`)
Introduce or preserve core-facing interfaces such as examples below:

- `AiContentGenerationPort`
- `PromptedProjectRequestAnalysisPort`
- `CandidateMatchExplanationPort`
- `QueryInterpretationPort`
- `EmbeddingPort`

These names can be adjusted to match the existing codebase, but the structure must be clean.

The core services in `candidate-match` should depend on **ports/interfaces**, not on Gemini/OpenAI/Ollama/Anthropic classes directly.

### AI side (`ai-rag-service`)
Implement those ports with adapters such as:

- `Gemini...Adapter`
- `OpenAi...Adapter`
- `Ollama...Adapter`
- `Anthropic...Adapter`
- `...EmbeddingAdapter`
- `...PromptService` or `...PromptTemplateProvider`

The AI module should contain:
- provider selection
- external HTTP/API wiring
- prompt rendering
- fallback/retry logic
- AI config properties

---

## Testing rules for the refactor

### Keep in `candidate-match`
Keep or rewrite tests that verify:
- matching rules
- scoring logic
- project request domain behavior
- transactional orchestration
- repository behavior
- controller request/response behavior where the controller belongs to core API
- business service behavior using mocked ports

### Move to `ai-rag-service`
Move or create tests that verify:
- Gemini/OpenAI/Ollama/Anthropic HTTP request construction
- provider factory selection
- prompt rendering
- embedding adapter behavior
- JSON parsing of AI responses
- AI fallback logic
- RAG integration details

### TDD requirement
For every moved or changed behavior:
1. preserve the existing test intent
2. rewrite tests in the target module where appropriate
3. keep business-logic assertions in core
4. do not lose coverage during the move

---

## `teknologi-barometer-service` requirement

For now, do not invent a large new domain there.

Instead:
- keep it lightweight
- align its dependency approach with the same style as `ai-rag-service`
- make it a satellite module with a clean dependency boundary
- do not move core repositories or business ownership there

If a dependency is needed, do it in the same architectural style as the AI module, without creating cyclic dependencies.

---

## Deliverables

Produce the refactor as a real code change with the following output sections:

1. **What I looked at & why**
   - list the files/packages scanned and why

2. **Findings**
   - summarize the current architectural issues
   - identify direct infrastructure coupling in `candidate-match`
   - explicitly call out any circular dependency risk

3. **Recommendations (ranked)**
   - each recommendation in one line:
     - change
     - benefit
     - principle

4. **Design sketch**
   - show target module boundaries
   - show ports/adapters
   - show dependency direction
   - explicitly explain how circular dependency is avoided

5. **Code changes (diff-ready)**
   - provide patch-style diffs
   - create/move files as needed
   - update package names and imports
   - update Maven module dependencies and POMs
   - if necessary, introduce a tiny shared contracts module to avoid cycles

6. **Tests**
   - move and rewrite tests as needed
   - keep business tests in core
   - move AI adapter tests to `ai-rag-service`
   - ensure tests compile and pass

7. **Build and module wiring**
   - update parent/module POMs
   - ensure the reactor still builds
   - ensure Spring component scanning / bean registration still works

8. **Assumptions**
   - clearly list any assumptions made

---

## Additional constraints

- Use DDD, SOLID, Clean Architecture, and modular-monolith principles
- Do not leak infrastructure models into the core domain
- Controllers must remain thin
- Keep code identifiers in English
- Keep user-facing messages/logs in Norwegian where already appropriate
- Prefer compile-safe refactoring over broad speculative redesign
- Preserve existing behavior unless a change is required for architectural correctness
- If you find an existing bug during the move, isolate it and fix it with a test first

---

## Specific implementation guidance

### Step 1
Scan current AI-related code in `candidate-match` and categorize each file as one of:
- core business logic → stays
- AI infrastructure adapter → moves to `ai-rag-service`
- prompt template / prompt builder → moves to `ai-rag-service`
- core-facing abstraction → stays in core or moves to shared contracts if needed

### Step 2
Introduce clean ports in the core for the AI use cases currently invoked directly.

### Step 3
Move provider-specific adapters into `ai-rag-service` and implement those ports there.

### Step 4
Move prompt templates and prompt rendering into `ai-rag-service`.

### Step 5
Refactor `candidate-match` services so they depend on ports only.

### Step 6
Move tests accordingly:
- adapter tests → `ai-rag-service`
- business logic tests → `candidate-match`

### Step 7
Update Maven dependencies carefully.
Do not create a cycle.
If required, extract a tiny contracts module.

### Step 8
Run/build mentally and provide the final diffs in a compile-oriented way.

---

## Important note on build strategy

If the current module graph prevents a clean move of AI adapters into `ai-rag-service` without a circular dependency, do **not** force an invalid design.

Instead, prefer this solution order:

1. try ports in core + implementations in `ai-rag-service`
2. if cycle appears, introduce a small contracts/shared-ports module
3. only if absolutely necessary, propose a small executable aggregator module

Explain the choice you make.

---

## Also inspect for known instability during the refactor

There is at least one fragile AI integration area in Anthropic/WebClient behavior.
Do not ignore it.
If touched, cover it with tests and keep the refactor safe.

---

## Success criteria

The refactor is successful when:

- `candidate-match` no longer contains provider-specific Gemini/OpenAI/Ollama/Anthropic adapter infrastructure
- prompt templates are no longer hard-coded in core services
- repositories remain in `candidate-match`
- core business logic tests remain in `candidate-match`
- AI adapter/prompt tests are moved to `ai-rag-service`
- the Maven module graph is clean
- the architecture becomes more modular without becoming a distributed microservice system
- the full project remains buildable