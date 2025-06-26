package no.cloudberries.candidatematch.templates

object MatchPromptTemplate {

    val template: String = """
        # 📊 Konsulentvurdering for {{consultantName}}

        ## Kundeforespørsel

        ```text
        {{request}}
        ```

        ---

        ## Konsulent CV: {{consultantName}}

        ```text
        {{cv}}
        ```

        ---

        ## Matchanalyse

        Gjør en grundig vurdering av hvor godt **{{consultantName}}** matcher kravene i forespørselen.

        - Hvert enkelt krav skal vurderes med begrunnelse
        - Score **1.0-10**
        - Resultatet skal være profesjonelt og kritisk, men konstruktivt
        - Ignorer krav til sikkerhetsklarering

        ---

        ## Format på forventet JSON-respons

        Returner et gyldig JSON-objekt med følgende struktur:

        {
          "totalScore": "X,X/10",
          "matchTimeSeconds": 42,
          "requirements": [
            {
              "name": "Beskrivelse av krav",
              "score": 8,
              "comment": "Faglig vurdering av hvordan kandidaten matcher dette kravet"
            }
          ],
          "summary": "Oppsummering av vurdering og eventuell anbefaling"
        }

        Ikke inkluder annen tekst utenfor JSON-objektet.
        Fjern backtickes ```json og avsluttende ```for gyldig json
        
    """.trimIndent()
}