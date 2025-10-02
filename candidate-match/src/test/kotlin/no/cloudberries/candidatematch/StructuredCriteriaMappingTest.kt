package no.cloudberries.candidatematch

import no.cloudberries.candidatematch.dto.ai.StructuredCriteria
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StructuredCriteriaMappingTest {

    @Test
    fun `StructuredCriteria maps industriesAny and publicSector into RelationalSearchCriteria`() {
        val sc = StructuredCriteria(
            skillsAll = listOf("kotlin"),
            publicSector = true,
            customersAny = listOf("sparebank1"),
            industries = listOf("finance")
        )
        val rc = sc.toRelationalSearchCriteria()
        assertEquals(true, rc.publicSector)
        assertEquals(listOf("sparebank1"), rc.customersAny)
        assertEquals(listOf("finance"), rc.industriesAny)
    }
}
