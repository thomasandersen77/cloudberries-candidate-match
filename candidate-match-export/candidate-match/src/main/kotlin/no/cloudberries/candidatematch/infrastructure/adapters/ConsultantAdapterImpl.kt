package no.cloudberries.candidatematch.infrastructure.adapters

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.delay
import no.cloudberries.candidatematch.domain.consultant.*
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseUserResponse
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.toDomain
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.toYear
import no.cloudberries.candidatematch.infrastructure.repositories.ConsultantRepository
import org.springframework.stereotype.Component

@Component
class ConsultantAdapterImpl(
    private val flowcaseHttpClient: FlowcaseHttpClient,
    private val consultantRepository: ConsultantRepository,
) : ConsultantAdapter {
    private val logger = mu.KotlinLogging.logger { }
    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .writerWithDefaultPrettyPrinter()

    override fun fetchAllConsultants(): List<CvUserInfo> = flowcaseHttpClient.fetchAllUsers()
        .flowcaseUserDTOs.map { user ->
            CvUserInfo(
                userId = user.userId,
                cvId = user.cvId,
                name = user.name,
                bornYear = user.bornYear,
                email = user.email
            ).also { logger.info { "Fetched user ${user.name} with userId ${user.userId} and cvId ${user.cvId}" } }
        }

    override fun fetchCompleteCv(userId: String, cvId: String): Cv = flowcaseHttpClient.fetchCompleteCv(
        userId = userId,
        cvId = cvId
    ).toDomain()
        .also { logger.info { "Fetched CV for userId $userId / cvId $cvId" } }


    override fun fetchConsultant(userId: String): Consultant {
        val response = flowcaseHttpClient.fetchUserById(userId)
        if (response is FlowcaseUserResponse.NotFound) {
            throw RuntimeException("User with userId $userId not found in Flowcase API")
        }

        val user = (response as FlowcaseUserResponse.Found).userDTO
        val completeCv = fetchCompleteCv(
            userId = user.userId,
            cvId = user.cvId
        )
        return Consultant.builder(
            id = user.userId,
            defaultCvId = user.cvId
        )
            .withCv(completeCv)
            .withCvAsJson(mapper.writeValueAsString(completeCv))
            .withPersonalInfo(
                PersonalInfo(
                    name = user.name,
                    email = user.email,
                    user.bornYear.toYear(),
                )
            )
            .withSkills(completeCv.skillCategories.flatMap { cat ->
                cat.skills
            })
            .build()
    }


    override suspend fun fetchConsultantsWithCv(): List<Consultant> = run {
        val cvUserInfos = fetchAllConsultants()
        val consultantsList = mutableListOf<Consultant?>()

        cvUserInfos.forEach {
            consultantsList.add(
                try {
                    delay(500)
                    fetchConsultant(it.userId)
                } catch (e: Exception) {
                    logger.warn(e) { "Could not fetch CV for ${it.name} with userId ${it.userId} / cvId ${it.cvId}. Exception: ${e.message}" }
                    null
                }
            )
        }

        return consultantsList.filterNotNull().toList()
    }

    override fun exists(userId: String): Boolean {
        val userExists = consultantRepository.existsByUserId(userId)
        return if (userExists) {
            logger.info { "User with userId $userId already exists in database" }
            true
        } else {
            false
        }
    }
}