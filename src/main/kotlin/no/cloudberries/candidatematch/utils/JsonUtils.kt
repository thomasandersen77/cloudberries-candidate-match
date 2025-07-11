package no.cloudberries.candidatematch.utils

import com.fasterxml.jackson.core.type.TypeReference
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseCvDTO

val flowcaseCvDTOListTypeRef = object : TypeReference<List<FlowcaseCvDTO>>() {}
