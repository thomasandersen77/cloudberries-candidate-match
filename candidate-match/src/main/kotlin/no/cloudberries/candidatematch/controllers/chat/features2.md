# Implementert:

1) Historikk i prompt/forespørsel for ikke-RAG-modus
   •  I AISearchOrchestrator hentes nå samtalehistorikk (siste ~5 turer) via ConversationService for hver forespørsel:
   ◦  STRUCTURED: Hvis vi oppdager et konsulentnavn i spørsmålet eller i historikken, biaser vi strukturelt søk ved å sette name i RelationalSearchCriteria (criteria.copy(name=...)). Historikk brukes ikke direkte i DB-spørringen, men lagres for kontekst og kan utvides senere.
   ◦  SEMANTIC: Vi bygger et augmentedText:
   ▪  base: interpretation.semanticText
   ▪  History: Q/A-sammendrag (kortet ned)
   ▪  Context: hvis navnedeteksjon finner en konsulent, injiseres en kompakt ferdighetsliste for vedkommende
   ▪  Dette augmentedText embeddes og brukes i semantisk søk.
   ◦  HYBRID: Re-ranking queryText bygges på samme måte (augmented med historikk og ev. konsulentkontekst) før reRankWithinCandidates.

2) Enkel navnedeteksjon + injeksjon av konsulent-kontext
   •  detectTargetConsultant(userText, history) prøver:
   ◦  findByNameContainingIgnoreCase(userText) (PageRequest.of(0,1))
   ◦  Hvis ikke treff, forsøker samme mot de siste spørsmålene i historikken (nyeste først).
   ◦  Returnerer Pair(name, userId) om funnet.
   •  buildConsultantContext(userId):
   ◦  Henter CV-er med ConsultantWithCvService.getCvsByUserId(userId, onlyActiveCv=true)
   ◦  Tar første CV og samler skill-navn fra skillCategories
   ◦  Bygger kort streng “Consultant userId=<id> skills: a, b, c …” (maks ~15 skill)
   •  augmentQuery(base, history, consultantContext):
   ◦  Slår sammen baseText, history (Q/A-linjer), optional context, og beskjærer for å holde det kompakt.

3) Konsekvenser
   •  SEMANTIC/HYBRID resultater vil kunne bli mer relevante i oppfølgingsspørsmål fordi embeddings tar hensyn til historikk og ev. målrettet konsulent-kontekst.
   •  STRUCTURED får navn-bias når et navn oppdages, men ellers uendret logikk.
   •  RAG var allerede historikk-basert; uendret.

Endrede filer (hovedpunkter)
•  AISearchOrchestrator.kt
◦  Ny avhengighet: ConversationService, ConsultantRepository, ConsultantWithCvService
◦  Nye hjelpemetoder: detectTargetConsultant, buildConsultantContext, augmentQuery
◦  Oppdatert executeStructuredSearch/executeSemanticSearch/executeHybridSearch til å hente historikk og injisere kontekst der det gir mening

Byggstatus
•  mvnw -DskipTests package: OK

Hva du kan teste
•  Start server og prøv en chat-sekvens:
1) POST /api/chatbot/search med text “Finn backend konsulenter med Kotlin” → svar inneholder conversationId
2) POST på nytt med samme conversationId og text “Hva med Thomas?” → orchestratoren vil prøve å detektere “Thomas” i tekst/historikk og hente ferdigheter for å injisere i semantic/hybrid og bias i structured
   •  SEMANTIC: verifiser at resultater gir meta med semanticScore og at resultatene virker mer målrettede i oppfølgingsspørsmål.
   •  HYBRID: se at re-ranking fortsatt fungerer, men queryText i re-rank er “augmented”.

Vil du at jeg også:
•  Legger inn flere signaler i structured-søk (for eksempel å mappe deler av historikk til customersAny/industriesAny) eller
•  Lager en liten navn-ordbok/cache for raskere navnedeteksjon?



# Key Features in the Updated OpenAPI Spec:

1. Chat Search Functionality (/api/chatbot/search):
   ◦  Support for conversationId to maintain chat history
   ◦  consultantId and cvId parameters for targeted searches
   ◦  Multiple search modes: STRUCTURED, SEMANTIC, HYBRID, RAG
   ◦  Comprehensive response with results, sources, and debug info
2. Conversation Management:
   ◦  conversationId field in ChatSearchRequest for maintaining chat context
   ◦  conversationId returned in responses for follow-up queries
3. Enhanced Search Features:
   ◦  Support for consultant targeting in searches
   ◦  RAG (Retrieval Augmented Generation) mode for specific consultant questions
   ◦  Debug information and timing data
   ◦  Scoring information for hybrid searches
4. Matches API Extensions:
   ◦  /api/matches/requests - List project requests with coverage info
   ◦  /api/matches/requests/{id}/top-consultants - Get AI-enriched top consultants
   ◦  New DTOs like MatchesListItemDto, PagedMatchesListDto, MatchConsultantDto
   ◦  CoverageStatus enum (GREEN, YELLOW, RED, NEUTRAL)
5. String-based IDs:
   ◦  consultantId and chunkId are now defined as strings instead of UUIDs
   ◦  This resolves the "Invalid UUID string" errors you mentioned

The frontend can now regenerate its TypeScript types based on this updated OpenAPI specification to stay in sync with the backend changes.