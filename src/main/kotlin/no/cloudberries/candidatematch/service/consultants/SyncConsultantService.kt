package no.cloudberries.candidatematch.service.consultants

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseCvDto
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.infrastructure.integration.flowcase.FlowcaseUserDTO
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SyncConsultantService(
    val flowcaseHttpClient: FlowcaseHttpClient
) {

    private val logger = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 */8 * * *")
    fun fetchFullCvForUser(){
        runBlocking { logger.info { "Running scheduled job to fetch full CV for all users"}
            flowcaseHttpClient.fetchAllUsers().flowcaseUserDTOs.forEach {
                delay(10)
                val cv = fetchCvForUser(
                    it.userId,
                    it.cvId
                )
            }
        }
    }

    fun fetchCvForUser(userid: String, cvId: String): FlowcaseCvDto = flowcaseHttpClient.fetchCompleteCv(userid, cvId)

    fun fetchUsers(): List<FlowcaseUserDTO> {
        val response = flowcaseHttpClient.fetchAllUsers()
        return response.flowcaseUserDTOs
    }

}