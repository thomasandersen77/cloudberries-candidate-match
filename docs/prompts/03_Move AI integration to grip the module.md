📝 Prompt til Juni: Flytting av AI-logikk til ai-rag-service
Kontekst:
Vi refaktorerer cloudberries-candidate-match til en modulær monolitt. Vi ønsker å samle all kommunikasjon med LLM-er og RAG-funksjonalitet i modulen ai-rag-service. candidate-match (Core) skal beholde Flowcase-integrasjonen, men delegere AI-oppgaver til den nye modulen.

Oppgave:

Flytt følgende pakker/filer fra candidate-match til ai-rag-service:

no.cloudberries.candidatematch.infrastructure.integration.openai (og tilsvarende for Gemini og Ollama).

no.cloudberries.candidatematch.infrastructure.integration.ai (inkludert AIContentGeneratorFactory).

no.cloudberries.candidatematch.domain.ai (AI-grensesnitt).

no.cloudberries.candidatematch.service.ai (AI-tjenester som AISearchOrchestrator, ConversationService, etc.).

Merk: Det ligger allerede en RagService.kt i ai-rag-service. Slå sammen logikken fra Core sin RAGService inn i denne og slett duplikater.

Håndter avhengigheter (Sirkulær avhengighet-sjekk):

Sørg for at ai-rag-service IKKE avhenger av candidate-match.

Hvis AI-modulen trenger modeller som AIResponse eller AISuggestion, flytt disse til ai-rag-service eller en ny shared-modul dersom de brukes tungt begge steder.

Forretningsmodeller som Consultant skal bli værende i Core. AI-modulen bør i størst mulig grad operere på "raw text" eller spesifikke AI-DTOer for å holde seg frakoblet domenet.

Oppdater Konfigurasjon:

Flytt AI-relaterte innstillinger fra application.yaml i Core til ai-rag-service.

Oppdater pom.xml i candidate-match slik at den har en avhengighet til ai-rag-service.

Sørg for at bønner (Beans) i ai-rag-service blir skannet korrekt når applikasjonen starter (bruk f.eks. @Import eller sjekk ScanBasePackages).

Verifisering:

Kjør mvn clean install for å sikre at alt kompilerer.

Sjekk at CandidateMatchingService i Core fortsatt kan kalle AI-funksjonalitet via de flyttede tjenestene.