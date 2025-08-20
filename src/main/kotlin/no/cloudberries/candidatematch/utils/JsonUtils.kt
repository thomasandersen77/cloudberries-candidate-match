package no.cloudberries.candidatematch.utils

import com.fasterxml.jackson.core.type.TypeReference
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseCvDto

val flowcaseCvDTOListTypeRef = object : TypeReference<List<FlowcaseCvDto>>() {}
