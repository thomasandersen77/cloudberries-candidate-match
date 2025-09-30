package no.cloudberries.candidatematch.service.consultants

import com.fasterxml.jackson.databind.JsonNode
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import org.springframework.stereotype.Service

@Service
class ConsultantCvService(
    private val consultantRepository: ConsultantRepository,
) {
    fun getCvForUser(userId: String): JsonNode {
        val entity = consultantRepository.findByUserId(userId)
            ?: throw NoSuchElementException("Consultant with userId=$userId not found")
        return entity.resumeData
    }
}