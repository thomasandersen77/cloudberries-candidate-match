package no.cloudberries.candidatematch.templates

object AnalyzeCustomerRequestPromptTemplate {
    // Keep simple and robust; frontend/dev can evolve template over time
    val template: String = """
    # 🤖 Kundeforespørsel – Analyse og strukturering

    Du er en ekspert på å analysere kundeforespørsler og identifisere krav til konsulenter.
    Oppgaven er å:
    1) Oppsummere forespørselen kortfattet (3–6 setninger)
    2) Liste krav som MÅ (MUST)
    3) Liste krav som BØR (SHOULD)

    Inndata (kundeforespørsel):
    {{request_text}}

    Returner på norsk, i ren tekst (ikke JSON), med tydelige seksjoner:
    - Oppsummering:
    - MUST-krav:
    - SHOULD-krav:
    """.trimIndent()
}

// Params and renderer for Project Request analysis

data class ProjectRequestParams(
    val requestText: String,
)

fun renderProjectRequestTemplate(template: String, params: ProjectRequestParams): String {
    return template.replace("{{request_text}}", params.requestText)
}