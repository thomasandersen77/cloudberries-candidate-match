package no.cloudberries.candidatematch.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "search.lexicon")
data class SearchLexiconProperties(
    var publicSectorTokens: List<String> = listOf(
        "kommune", "etat", "nav", "skatt", "stat", "offentlig", "departement", "direktorat",
        "helsedirektoratet", "kartverket", "politiet", "helse", "utdanningsdirektoratet"
    ),
    var customerSynonyms: Map<String, List<String>> = mapOf(
        "sparebank1" to listOf("sparebank 1", "sb1"),
        "dnb" to listOf("den norske bank", "d.n.b"),
        "nav" to listOf("arbeids- og velferdsetaten")
    ),
    var industrySynonyms: Map<String, List<String>> = mapOf(
        "finance" to listOf("bank", "finans", "sparebank", "sparebank1", "dnb"),
        "public" to listOf("kommune", "etat", "offentlig", "departement", "direktorat", "nav"),
        "health" to listOf("helse", "sykehus", "helsedirektoratet")
    )
)

@Configuration
class SearchLexicon(val props: SearchLexiconProperties) {
    val publicSectorTokens: List<String> get() = props.publicSectorTokens
    fun expandCustomerTerm(term: String): List<String> = listOf(term) + (props.customerSynonyms[term.lowercase()] ?: emptyList())
    fun detectIndustries(text: String?): Set<String> {
        if (text.isNullOrBlank()) return emptySet()
        val lower = text.lowercase()
        val hits = mutableSetOf<String>()
        props.industrySynonyms.forEach { (canonical, syns) ->
            if (syns.any { s -> lower.contains(s.lowercase()) }) hits.add(canonical)
        }
        return hits
    }
}
