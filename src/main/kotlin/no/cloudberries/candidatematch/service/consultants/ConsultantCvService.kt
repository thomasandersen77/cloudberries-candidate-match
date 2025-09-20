package no.cloudberries.candidatematch.service.consultants

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import org.springframework.stereotype.Service

@Service
class ConsultantCvService(
    private val consultantRepository: ConsultantRepository,
) {
    fun getCvForUser(userId: String): JsonNode {
        val entity = consultantRepository.findByUserId(userId)
            ?: throw NoSuchElementException("Consultant with userId=$userId not found")
        val node = entity.resumeData.deepCopy<JsonNode>()
        if (node is ObjectNode) {
            val copy = node.deepCopy<ObjectNode>()
            copy.put(
                "displayName",
                entity.name
            )
            return copy
        }
        return node
    }
}