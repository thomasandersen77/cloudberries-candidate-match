---
apply: always
---

# Cloudberries Candidate Match Backend Rules (Canonical)

---

## 0. Rule Contract (Always Enforced)

These rules must be followed for every change in this project:

1.  **Modular Monolith Architecture**
    *   `candidate-match`: Core service for CV analysis, project request matching, and consultant management.
    *   `ai-rag-service`: Dedicated service for RAG (Retrieval-Augmented Generation) and vector operations.
    *   `teknologi-barometer-service`: Service for analyzing technology trends.
    *   Packages follow: `controllers`, `service`, `domain`, `infrastructure`, `dto`.

2.  **Layering (Strict)**
    *   `Controller` → `Service` (Domain/Application)
    *   `Service` → `Port` (Interface) or `Repository`
    *   `Port` → `Infrastructure Adapter` (External APIs, AI Providers)

3.  **Ultra-thin Controllers**
    *   Controllers are responsible for: routing, extracting auth context, syntactic validation, and mapping DTOs ↔ Domain objects.
    *   Controllers must NOT contain: business rules, direct database queries, complex domain logic, or AI orchestration.

4.  **Domain-Driven Design (DDD) & Clean Architecture**
    *   **Core Isolation**: Domain logic should be isolated in `no.cloudberries.candidatematch.domain`. It must not depend on infrastructure details (JPA, Web, AI SDKs).
    *   **Entities**: Represent objects with identity. Use `class` (not `data class`) for JPA entities to avoid lazy-loading issues in `equals`/`hashCode`.
    *   **Value Objects**: Represent descriptive aspects of the domain with no identity. Use `data class` and ensure immutability.
    *   **Aggregate Roots**: Use repositories only for aggregate roots (e.g., `ProjectRequest`, `Consultant`).
    *   **Ports & Adapters**: Define interfaces in the domain/service layer (Ports) and implement them in the infrastructure layer (Adapters).

5.  **Test-Driven Development (TDD) & Quality**
    *   **Bug Fixes**: ALWAYS write a reproduction test before fixing a bug.
    *   **Unit Tests**: Required for all business logic in services and domain models. Use **MockK** for mocking.
    *   **Integration Tests**: Use `@SpringBootTest` with Testcontainers (Postgres/pgvector) or H2 for database verification.
    *   **MockMvc**: Use for testing API contracts, security (RBAC), and DTO mapping.

6.  **AI & LLM Integration (Specialized Rules)**
    *   **Provider Abstraction**: Never couple domain logic directly to a specific LLM (Gemini, Anthropic). Use `EmbeddingProvider` or `AIClient` ports.
    *   **Prompt Management**: Keep prompt templates in dedicated classes/files (e.g., `no.cloudberries.candidatematch.templates`).
    *   **RAG Flow**: Vector searches should be performed via `ai-rag-service` or dedicated repository methods using `pgvector`.
    *   **Reliability**: Implement fallbacks and retries for AI calls.

7.  **Data Handling & PostgreSQL**
    *   **pgvector**: All vector embeddings must be stored in PostgreSQL using the `vector` type.
    *   **Money**: All monetary values must be represented as `Long` in minor units (e.g., øre). Floating-point types are **forbidden** for currency.
    *   **Transactions**: All state-changing service methods MUST be annotated with `@Transactional`.

8.  **Security & Identity**
    *   **Authentication**: Use `@CurrentUser` (or equivalent mechanism) in controller signatures.
    *   **Authorization**: Enforce RBAC (Role-Based Access Control) in the service layer.
    *   **DTO Boundary**: Never return domain entities or infrastructure entities directly in API responses.

9.  **Naming & Language**
    *   **English**: All code identifiers (classes, variables), database tables/columns, and technical documentation.
    *   **Norwegian**: Exception messages intended for end-users and functional/business log messages.

10. **SOLID Principles**
    *   **SRP**: Each class should have one reason to change.
    *   **DIP**: Depend on abstractions, not concretions (especially for AI and Repositories).

---

## 1. System Architecture Details

### 1.1 `candidate-match` Structure
- **`controllers`**: REST endpoints, thin, mapping only.
- **`service`**: Orchestrates use-cases, contains domain logic when it doesn't fit on an entity.
- **`domain`**: The "heart" – entities, value objects, and domain services.
- **`infrastructure`**: Implementation details (JPA repositories, AI client adapters, external integrations).

### 1.2 Multi-Service Interaction
- `candidate-match` is the orchestrator.
- External calls to `ai-rag-service` should be handled via a dedicated `Port` in `candidate-match`.

---

## 2. Testing Strategy

### 2.1 Unit Tests (Core)
- Focus: Testing complex business rules, scoring algorithms, and mapping logic.
- Tool: `JUnit 5`, `MockK`, `AssertJ`.
- Rule: No Spring context allowed in core unit tests.

### 2.2 Integration Tests
- Focus: Verifying JPA queries, pgvector operations, and service wiring.
- Tool: `@DataJpaTest` or `@SpringBootTest`.
- Rule: Ensure `pgvector` extension is available in the test database.

---

## 3. Requirement-Driven Development (RDD)

1.  **Source of Truth**: Functional requirements are defined in `docs/` and project overview files.
2.  **Verification**: Before starting a task, verify the requirement in the spec.
3.  **Validation**: After implementation, ensure the behavior matches the requirement (e.g., matching accuracy, skill extraction).

---

## 4. Multi-LLM Role Delegation

- **Junie (Implementer)**: Code changes, terminal operations, running tests, fixing bugs.
- **Claude (Architect)**: High-level design, complex logic planning, spec writing.
- **ChatGPT/Codex (Consultant)**: Documentation, library knowledge, SQL queries.

---
