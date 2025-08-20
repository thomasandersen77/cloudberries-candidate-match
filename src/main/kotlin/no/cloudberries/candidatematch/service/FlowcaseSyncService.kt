package no.cloudberries.candidatematch.service

import no.cloudberries.candidatematch.integration.flowcase.FlowcaseCvDto
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseUserDTO
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class FlowcaseSyncService(
    val flowcaseHttpClient: FlowcaseHttpClient
) {

    private val logger = mu.KotlinLogging.logger {}

    @Scheduled(cron = "0 0 */8 * * *")
    fun fetchFullCvForUser(){
        flowcaseHttpClient.fetchAllUsers().flowcaseUserDTOList.forEach {
            val cvDto = fetchCvForUser(
                it.userId,
                it.cvId
            )
            Thread.sleep(50)
        }
    }

    fun fetchCvForUser(userid: String, cvId: String): FlowcaseCvDto {
        val cv = flowcaseHttpClient.fetchCompleteCv(userid, cvId)
        logger.info  { "FlowcaseCV for ${cv.title?.text}: ${cv.name} Found ${cv.projectExperiences.map { p -> p.projectExperienceSkills.size }} skills..." }
        return cv
    }

    fun fetchUsers(): List<FlowcaseUserDTO> {
        val response = flowcaseHttpClient.fetchAllUsers()
        return response.flowcaseUserDTOList
    }

}