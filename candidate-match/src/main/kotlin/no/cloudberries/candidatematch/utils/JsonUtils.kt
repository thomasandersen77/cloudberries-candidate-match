package no.cloudberries.candidatematch.utils

import com.fasterxml.jackson.core.type.TypeReference
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseCvDto

val flowcaseCvDTOListTypeRef = object : TypeReference<List<FlowcaseCvDto>>() {}
