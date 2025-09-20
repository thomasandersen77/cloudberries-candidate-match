package no.cloudberries.candidatematch.templates

object AnalyzeCustomerRequestPromptTemplate {

    val template: String = """
    
    # 🤖 CV-Vurdering (Analytisk Modell)    
    
    # Kontektst
    
    Du er en ekspert på å analysere personlig og tekniske krav til en konsulent i en kundeforespørsle
    
    {{forespørsel}}
    
    """.trimIndent()
}