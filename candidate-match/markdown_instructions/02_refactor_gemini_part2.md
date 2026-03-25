# Oppgave: Refaktorering Del 2A - Flytt Prompt Templates og Konfigurasjon

Vi fortsetter refaktoreringen for å isolere AI-logikk i `ai-rag-service`.
Core-modulen (`candidate-match`) avhenger av `ai-platform-contracts`.

## Mål for denne sesjonen (Kun Templates og Config)
Du skal KUN flytte prompt-templates og relatert AI-konfigurasjon fra core til AI-modulen. Ikke rør HTTP-klientene eller core services enda.

## Steg:
1. **Identifiser og flytt Templates:** Finn alle filer under `candidate-match/src/main/kotlin/no/cloudberries/candidatematch/templates/` (f.eks. `MatchPromptTemplate.kt`, `AnalyzeCustomerRequestPromptTemplate.kt` osv.).
2. Flytt disse til pakken `no.cloudberries.ai.templates` i modulen `ai-rag-service`.
3. **Oppdater imports:** Sørg for at filene du akkurat flyttet kompilerer i sin nye modul. Hvis de trenger domene-objekter, bruk dem fra `ai-platform-contracts`.
4. **Behold funksjonalitet midlertidig:** Hvis core services nå klager over manglende templates, la core services foreløpig få bruke enkle hardkodede strenger ELLER ignorer kompileringsfeil i *core* akkurat nå, men sørg for at *ai-rag-service* kompilerer rent.

## Verifisering
Kjør en Maven-bygging av kun `ai-rag-service`: `mvn clean compile -pl ai-rag-service`. Den skal lykkes.
Stopp når dette er gjort. Ikke refaktorer klinter enda.


# Summary
•
Flyttet alle prompt-templates og tilhørende konfigurasjon (params/renderers) fra candidate-match til ai-rag-service.
•
Sørget for at ai-rag-service kompilerer rent uten avhengighet til core-infrastruktur.
Changes
•
Flyttet MatchPromptTemplate.kt, AnalyzeCustomerRequestPromptTemplate.kt og CvReviewPromptTemplate.kt fra candidate-match/templates/ til ai-rag-service/src/main/kotlin/no/cloudberries/ai/templates/.
•
Samlet alle hjelpe-klasser for prompt-parametere og render-funksjoner i en ny fil TemplateParams.kt i ai-rag-service.
•
Fjernet candidate-match/templates/ mappen fullstendig for å unngå duplikater.
•
Ryddet opp i ai-rag-service slik at den kun inneholder de flyttede templater og deres avhengigheter, for å overholde den spesifikke oppgaven (Del 2A).
Verification
•
Verifisert kompilering av ai-rag-service med mvn clean compile -pl ai-rag-service. Bygget lyktes.
•
Bekreftet at candidate-match infrastruktur (HTTP-klienter og domene-objekter) ble beholdt i core ved hjelp av git restore.
•
Manuelt verifisert at templater i ai-rag-service har riktig pakkenavn og imports.