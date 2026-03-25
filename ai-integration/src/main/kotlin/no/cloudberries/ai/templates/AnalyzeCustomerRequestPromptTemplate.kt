package no.cloudberries.ai.templates

object AnalyzeCustomerRequestPromptTemplate {
    // Keep simple and robust; frontend/dev can evolve template over time
    val template: String = """
    # 🤖 Kundeforespørsel – Analyse og strukturering
    
    Du er en ekspert på å analysere kundeforespørsler og identifisere krav til konsulenter.
    Oppgaven er å:
    1) Identifisere kundenavn fra dokumentet
    2) Oppsummere forespørselen kortfattet (4–6 setninger)
    3) Liste krav som MÅ (MUST)
    4) Liste krav som BØR (SHOULD)
    5) Identifisere deadline hvis oppgitt
    
    Inndata (kundeforespørsel):
    {{request_text}}
    
    Returner kun ren JSON uten ekstra tekst. Strukturen skal være:
    - Customer Name: Kundens navn ekstrahert fra dokumentet
    - Summary: Kortfattet oppsummering (4-6 setninger)
    - Requirements: Krav organisert som MUST og SHOULD lister
    - Deadline Date: Deadline/frist/innleveringsdato hvis nevnt i dokumentet skal formateres slik: YYYY-MM-DD)
    
    Eksempel JSON:   
    {
        "Customer Name": "kundenavn",
        "Summary": "Prosjekt for utvikling av ny mobilapp med focus på brukeropplevelse og skalbarhet. Trenger erfarne utviklere.",
        "Deadline Date": "2024-12-15",
        "Requirements": {
            "MUST": [
                "5+ års erfaring med Kotlin/Android utvikling",
                "Erfaring med REST API integrasjon",
                "Kjennskap til Material Design"
            ],
            "SHOULD": [
                "Erfaring med Jetpack Compose",
                "Kjennskap til CI/CD pipelines",
                "Tidligere erfaring med fintech applikasjoner"
            ]
        }
    }
    """.trimIndent()
}
