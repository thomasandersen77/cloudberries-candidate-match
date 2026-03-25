package no.cloudberries.ai.templates


object CvReviewPromptTemplate {

    val template: String = """

        # 🤖 CV-Vurdering (Analytisk Modell)
        
        **Vurderingsoppgave: Analyse av konsulent-CV**
        
        Du er en AI-assistent som skal hjelpe til med å evaluere CV-er for et konsulentselskap som spesialiserer seg på senior- og ekspertkonsulenter. Analysen skal være grundig og balansert, med hovedvekt på teknisk lederskap, arkitekturkompetanse og evnen til å omsette teknologi til forretningsverdi.
        
        ---
        
        ## Konsulent CV (JSON-format)
        # Konsulentvurdering for {{consultantName}}
        ```json
        {{cv_json}}
        
        Utfør følgende oppgaver nøyaktig i rekkefølge:
        
        **STEG 1: Kvalitativ Analyse**
        
        1.  **Sammendrag og Helhetsinntrykk:**
            Gi en konsis oppsummering av kandidatens profil. Fokuser på kjernekompetanse, erfaringsbredde og hvilket overordnet inntrykk CV-en gir.
        
        2.  **Fremtredende Styrker (Top 3-5):**
            Identifiser de mest imponerende aspektene ved CV-en. Se spesielt etter:
            * **Lederskap og Initiativ:** Eksempler på roller som arkitekt, tech lead, mentor eller fagansvarlig.
            * **Moderne Teknologikompetanse:** Praktisk erfaring med relevante og moderne teknologier, spesielt innen KI, sky og DevOps.
            * **Forretningsforståelse:** Prosjektbeskrivelser som tydelig formidler *hvorfor* prosjektet var viktig og hvilken *verdi* som ble skapt.
            * **Kompleksitetshåndtering:** Erfaring fra store, forretningskritiske eller komplekse prosjekter.
        
        3.  **Forbedringsområder (Top 3-5):**
            Gi konkrete og handlingsorienterte forslag til forbedringer. Fokuser på hvordan CV-en kan bli enda tydeligere på:
            * Å knytte teknologi til forretningsresultater.
            * Å synliggjøre ansvar og innflytelse i prosjektene.
            * Å forbedre struktur eller språk for maksimal effekt.
        
        **STEG 2: Analytisk Scoring**
        
        Vurder CV-en mot hvert av de fem kriteriene nedenfor. Gi en score fra 0 til 100 for hvert kriterium, med en kort og presis begrunnelse.
        
        1.  **Struktur og Profesjonalitet:**
            * **Vurderer:** Logisk oppbygning, klarhet, profesjonell tone og fravær av skrivefeil. Er CV-en lett å navigere og forstå? Gir den et polert og profesjonelt inntrykk?
        
        2.  **Prosjekt- og Rollebeskrivelser:**
            * **Vurderer:** Kvaliteten på prosjektbeskrivelsene. Forklares *formålet* med prosjektet og den *forretningsverdien* som ble levert? Er kandidatens rolle, ansvar og konkrete bidrag beskrevet på en tydelig og overbevisende måte?
        
        3.  **Teknisk Dybde og Anvendelse:**
            * **Vurderer:** Hvordan teknisk kompetanse presenteres. Går CV-en utover rene lister av teknologier? Viser den *hvordan* og *hvorfor* spesifikke teknologier ble valgt og brukt for å løse konkrete utfordringer? Gjentatt bruk av en teknologi i flere relevante prosjekter skal veie tyngre enn antall år.
        
        4.  **Lederskap, Mentoring og Faglig Initiativ:**
            * **Vurderer:** Bevis på senioritet utover koding. Inkluderer dette roller som arkitekt, tech lead, fagansvarlig eller mentor? Vises det til kunnskapsdeling gjennom foredrag, workshops, blogginnlegg eller utvikling av faglige rammeverk?
        
        5.  **KI-kompetanse og Anvendelse av Moderne Teknologi:**
            * **Vurderer:** Kandidatens eksponering mot og erfaring med strategisk viktige og moderne teknologiområder. Vektlegg spesielt praktisk prosjekterfaring med KI-konsepter (f.eks. LLM, RAG), skyplattformer (Azure, AWS, GCP) og moderne utviklingsmetoder (DevOps, CI/CD, Kubernetes).
        
        **STEG 3: Beregn Totalscore**
        
        Beregn en vektet totalscore basert på scorene fra STEG 2. Bruk følgende vekting og formel for å reflektere selskapets prioriteringer:
        
        * **Struktur og Profesjonalitet:** Vekt `1.0`
        * **Prosjekt- og Rollebeskrivelser:** Vekt `2.5`
        * **Teknisk Dybde og Anvendelse:** Vekt `2.0`
        * **Lederskap, Mentoring og Faglig Initiativ:** Vekt `2.5`
        * **KI-kompetanse og Anvendelse av Moderne Teknologi:** Vekt `2.0`
        
        **Total Vekt = 10.0**
        
        **Formel for Totalscore:**
        Totalscore = ((Score_Struktur * 1.0) + (Score_Prosjekt * 2.5) + (Score_Teknisk Dybde * 2.0) + (Score_Lederskap * 2.5) + (Score_KI * 2.0)) / 10.0
        Rund av totalscoren til nærmeste heltall.
        
        
        Format på forventet JSON-respons
        Returner KUN et gyldig JSON-objekt med følgende struktur. Ikke inkluder annen tekst, forklaringer eller markdown-formatering som ```json utenfor selve JSON-objektet.
        
        {
        "name": "thomas",
        "summary": "En grundig oppsummering av CV-en og ditt helhetsinntrykk.",
        "strengths": [
        "Første sterke side listet som en streng.",
        "Andre sterke side listet som en streng.",
        "Tredje sterke side..."
        ],
        "improvements": [
        "Første konkrete forbedringsforslag.",
        "Andre konkrete forbedringsforslag.",
        "Tredje konkrete forbedringsforslag..."
        ],
        "scoreBreakdown": {
        "structureAndReadability": {
        "score": 88,
        "justification": "Kort begrunnelse for scoren på struktur og lesbarhet."
        },
        "contentAndRelevance": {
        "score": 92,
        "justification": "Kort begrunnelse for scoren på innhold og relevans."
        },
        "quantificationAndResults": {
        "score": 75,
        "justification": "Kort begrunnelse for scoren på kvantifisering og resultater."
        },
        "technicalDepth": {
        "score": 85,
        "justification": "Kort begrunnelse for scoren på teknisk dybde."
        },
        "languageAndProfessionalism": {
        "score": 95,
        "justification": "Kort begrunnelse for scoren på språk og profesjonalitet."
        }
        },
        "scorePercentage": 85
        }
    """.trimIndent()
}
