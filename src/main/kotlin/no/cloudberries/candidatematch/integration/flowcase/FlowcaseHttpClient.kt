package no.cloudberries.candidatematch.integration.flowcase

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.cloudberries.candidatematch.domain.consultant.Cv
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class FlowcaseHttpClient(
    private val config: FlowcaseConfig
) {
    private val logger = mu.KotlinLogging.logger {}
    private val bearerToken = "Bearer ${config.apiKey}"
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val client = OkHttpClient.Builder()
        .connectTimeout(
            30,
            TimeUnit.SECONDS
        )
        .readTimeout(
            30,
            TimeUnit.SECONDS
        )
        .build()

    fun checkHealth(): Boolean {
        val request = Request.Builder()
            .url("${config.baseUrl}/v1/masterdata/custom_tags/custom_tag_category")
            .header(
                "Authorization",
                bearerToken
            )
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            return response.isSuccessful
        }
    }

    /**
     * Henter en liste med basis-informasjon for alle brukere/CV-er.
     * Denne gir oss `userId` og `cvId` som vi trenger for videre kall.
     */
    fun fetchAllUsers(): FlowcaseUserSearchResponse {
        val url = "${config.baseUrl}/v2/users/search?page=0&size=10000"
        val request = buildGetRequest(url)

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Error from Flowcase API (fetchAllCvOverviews): ${response.code} - ${response.message}")
            }
            val responseBodyString = response.body.string()

            return responseBodyString.let {
                val typeRef = object : TypeReference<List<FlowcaseUserDTO>>() {}


                val flowcaseUserDTOS = mapper.readValue(
                    it,
                    typeRef
                )
                logger.info { "Fetched ${flowcaseUserDTOS.size} users from Flowcase API" }

                FlowcaseUserSearchResponse(
                    flowcaseUserDTOS
                )
            }
        }
    }

    fun fetchCompleteCv(userId: String, cvId: String): FlowcaseCvDto {
        val url = "${config.baseUrl}/v3/cvs/$userId/$cvId"
        val request = buildGetRequest(url)
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.warn { "Warning: Could not fetch full resume for userId $userId / cvId $cvId. Status: ${response.code}" }

            }
            return response.body.string().let {
                mapper.readValue(
                    it,
                    FlowcaseCvDto::class.java
                ).also {
                    logger.info { "Fetched CV for userId $userId / cvId $cvId" }
                }
            }
        }
    }

    private fun buildGetRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .header(
                "Authorization",
                bearerToken
            )
            .header(
                "Accept",
                "application/json"
            )
            .build()
    }
}