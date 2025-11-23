package no.cloudberries.candidatematch

import no.cloudberries.candidatematch.config.SearchLexicon
import no.cloudberries.candidatematch.config.SearchLexiconProperties
import no.cloudberries.candidatematch.dto.ai.StructuredCriteria
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SearchLexiconTest {

    @Test
    fun `expandCustomerTerm returns synonyms and original`() {
        val props = SearchLexiconProperties(
            customerSynonyms = mapOf("sparebank1" to listOf("sparebank 1", "sb1"))
        )
        val lex = SearchLexicon(props)
        val expanded = lex.expandCustomerTerm("sparebank1")
        assertTrue(expanded.contains("sparebank1"))
        assertTrue(expanded.contains("sparebank 1"))
        assertTrue(expanded.contains("sb1"))
    }

    @Test
    fun `detectIndustries finds canonical from synonyms`() {
        val props = SearchLexiconProperties(
            industrySynonyms = mapOf("finance" to listOf("bank", "finans"))
        )
        val lex = SearchLexicon(props)
        val hits = lex.detectIndustries("Prosjekt i bank og finans for Sparebank1")
        assertTrue(hits.contains("finance"))
    }
}
