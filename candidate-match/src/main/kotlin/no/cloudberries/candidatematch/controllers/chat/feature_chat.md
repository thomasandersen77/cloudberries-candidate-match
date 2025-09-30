# Refactor chat

Det må lages en service som kan ta i mot prompten
Denne ska først tolke teksten som kommer inn, mulig det er stikkord ifbm en konsulent i teksten.
Det kan være spørsmål om teknologier eller roller. Fra friteksten, burde det gjøres et kall til Ai for
å sjekke hva det spørres om, eller kan vi programatisk tolke den og gjøre en semantisk
spørring mot vektor databasen eller bør vi bruke en kombinasjon med strukturert søk?

Tanken er å kunne sitte å chatte med AI om en eller flere konsulenter med CVene deres i konteksten - RAG

Gjør en grundig vurdering men vi bør ikke kreve mer en et prompt fra bruker i frontend.
Brukerem kunne fått en del ferdige spørsmål der de fyller ut placeholdere som vi vet hva vi skal gjøre
med når vi tar de imot

**Strukturert søk:**

```json
{
  "skillsAll": [
    "KOTLIN",
    "SPRING"
  ],
  "skillsAny": [
    "ARCHITECTURE",
    "TECH_LEAD"
  ],
  "minQualityScore": 85
}
```

*"Konsulenter som KAN Kotlin og Spring, og som OGSÅ har arkitektur- eller tech-lead-erfaring, med score over 85"*

**Semantisk søk:**

```json
{
  "text": "Erfaren fullstack-utvikler som kan mentorere juniorer",
  "topK": 5
}
```

*"5 beste konsulenter basert på semantisk matching av beskrivelsen"*

```kotlin

@RestController
@RequestMapping("/api/chatbot")
class AIController(
    private val aiAnalysisService: AIAnalysisService
) {
    private val logger = KotlinLogging.logger { }

    @PostMapping("/analyze")
    @Timed
    fun analyzeContent(
        @RequestBody request: AIAnalysisRequest,
    ): AIResponse {
        logger.info { "POST /api/chatbot/analyze content.len=${request.content.length}" }

        return aiAnalysisService.analyzeContent(
            request.content,
            AIProvider.GEMINI
        )
    }
}

data class AIAnalysisRequest(
    val content: String
)
```

Gjør en grundig vurdering
Følg controller - service -repository patten
Mulig du trenger noen mer avansert logikk, skille det ut i en egen service

Se dette i sammenheng med eksisterende arkitektur.
Hastighet på svar er kritisk, mulig vi burde bruke en raskere LLM fra Gemini hvis vi må kalle AI først før db. Vi ønsker
å høy responsiviteten på chatten.

🎉 Implementation Summary

I've successfully implemented the core AI-powered consultant search system! Here's what has been completed:

✅ Completed Features

1. Environment & Dependencies
    * ✅ Created feature branch feat/ai-consultant-search
    * ✅ Java 21.0.7-tem configured via SDKMAN
    * ✅ Database running with pgvector support
    * ✅ AI configuration with Gemini Flash and Pro models

2. Core Architecture Implemented
    * ✅ ChatSearchRequest/Response DTOs with comprehensive schemas
    * ✅ AIQueryInterpretationService using Gemini Flash for fast query classification
    * ✅ AISearchOrchestrator routing queries between different search modes
    * ✅ Enhanced AIController with new /api/chatbot/search endpoint

3. Search Modes Implemented
    * ✅ Structured Search: Uses existing ConsultantSearchService for precise skill/role filtering
    * ✅ Semantic Search: Leverages existing vector embeddings for natural language queries
    * ✅ Hybrid Search: Combines structured filtering + semantic ranking
    * 🔄 RAG Search: Placeholder implemented (returns helpful message)

4. Smart Query Processing
    * ✅ Skill normalization with synonyms (Kotlin/Spring/React/etc.)
    * ✅ Confidence scoring for route selection
    * ✅ Fallback mechanisms when AI or search fails
    * ✅ Caching for query interpretations (5-15 min TTL)

5. API & Documentation
    * ✅ OpenAPI specification updated with comprehensive schemas
    * ✅ Frontend sync: openapi.yaml copied to frontend project
    * ✅ Test script created for endpoint validation

🧪 Ready to Test

The system can now handle these query examples:

```bash
# Structured search
"Find consultants who know Kotlin and Spring"

# Semantic search  
"Experienced fullstack developer who can mentor juniors"

# Forced mode
{"text": "Java developer", "forceMode": "semantic"}

# RAG (returns helpful message for now)
"Tell me about Thomas Andersen's experience with React"
```

🔄 Next Steps for Full Implementation

### The remaining work focuses on RAG functionality and advanced features:

1. RAG Components: CV chunking, vector storage, and answer generation
2. Conversation Memory: Session persistence for multi-turn chats
3. Performance Optimization: Caching, metrics, and monitoring
4. Testing Suite: Unit, integration, and E2E tests

### 🚀 How to Test

```bash
# Start the application (ensure GEMINI_API_KEY is set)
mvn spring-boot:run

# Test the endpoint
./test_ai_search.sh
```

The foundation is solid and ready for the next phase of RAG implementation! The system intelligently routes queries and
integrates seamlessly with your existing search infrastructure.