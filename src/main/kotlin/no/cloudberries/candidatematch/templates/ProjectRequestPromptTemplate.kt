package no.cloudberries.candidatematch.templates

object ProjectRequestPromptTemplate {

    val template: String = """
        # 📄 Kundeforespørsel – Kravuttrekk

        Du er en erfaren løsningsarkitekt og anbudsrådgiver. Du skal analysere en kundeforespørsel (som fritekst) og trekke ut en strukturert liste over krav. Skill tydelig mellom må-krav (obligatoriske) og bør-krav (ønskede). Svaret skal returneres KUN som et gyldig JSON-objekt under root-nøkkelen "project_request".

        ## Kundeforespørsel (tekst)
        ```text
        {{request_text}}
        ```

        ---

        ## Instruksjoner
        - Les teksten og identifiser eksplisitte og implisitte krav.
        - Del resultatet i to lister: må-krav (must_requirements) og bør-krav (should_requirements).
        - Hvert krav skal inneholde navn (name) og en kort utdyping (details).
        - Forsøk å hente ut kundens navn (customer_name), tittel på forespørselen (title) og en kort oppsummering (summary) hvis mulig.
        - Returner KUN JSON, ingen forklarende tekst, ingen markdown-kodeblokker.

        ## Forventet JSON-format
        {
          "project_request": {
            "customer_name": "Kunde AS",
            "title": "Senior Kotlin-utvikler",
            "summary": "Kort oppsummering av behovet.",
            "must_requirements": [ { "name": "Kotlin", "details": "5+ år erfaring, Spring Boot" } ],
            "should_requirements": [ { "name": "React", "details": "Erfaring med frontend er en fordel" } ]
          }
        }

        Ikke inkluder annen tekst utenfor JSON-objektet. Fjern eventuelle ```-markører.
    """.trimIndent()
}