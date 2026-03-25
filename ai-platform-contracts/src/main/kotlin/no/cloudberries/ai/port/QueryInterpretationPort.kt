package no.cloudberries.ai.port

import no.cloudberries.ai.dto.QueryInterpretation
import no.cloudberries.ai.dto.SearchMode

interface QueryInterpretationPort {
    fun interpretQuery(userText: String, forceMode: SearchMode? = null): QueryInterpretation
}