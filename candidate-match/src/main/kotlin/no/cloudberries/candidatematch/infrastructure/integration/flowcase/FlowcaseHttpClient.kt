package no.cloudberries.candidatematch.infrastructure.integration.flowcase

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class FlowcaseHttpClient(
    private val config: FlowcaseConfig
) {
    private val logger = KotlinLogging.logger {}
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

    fun fetchUserById(userId: String): FlowcaseUserResponse {
        val url = "${config.baseUrl}/v1/users/$userId"
        val request = buildGetRequest(url)
        logger.info { "Fetching user with userId $userId and url $url" }
        client.newCall(request).execute().use { response ->
            when {
                response.isSuccessful -> {
                    return response.body.string().let {
                        FlowcaseUserResponse.Found(
                            mapper.readValue(
                                it,
                                FlowcaseUserDTO::class.java
                            ).also {
                                logger.debug { "Fetched user with userId $userId" }
                            }
                        )
                    }
                }

                response.code == 404 -> {
                    logger.error { "User with userId $userId not found in Flowcase API" }
                    return FlowcaseUserResponse.NotFound
                }

                response.code == 429 -> {
                    logger.warn { "Rate limit exceeded for fetchUserById($userId). Consider implementing retry logic." }
                    throw RuntimeException("Error from Flowcase API (fetchUserById($userId)): ${response.code} - Rate limit exceeded")
                }

                else -> {
                    throw RuntimeException("Error from Flowcase API (fetchUserById($userId)): ${response.code} - ${response.message}")
                }
            }
        }
    }

    fun fetchCompleteCv(userId: String, cvId: String): FlowcaseCvDto {
        val url = "${config.baseUrl}/v3/cvs/$userId/$cvId"
        val request = buildGetRequest(url)
        client.newCall(request).execute().use { response ->
            when {
                response.isSuccessful -> {
                    return response.body.string().let {
                        mapper.readValue(
                            it,
                            FlowcaseCvDto::class.java
                        ).also {
                            logger.debug() { "Fetched CV for userId $userId / cvId $cvId" }
                        }
                    }
                }

                response.code == 429 -> {
                    logger.warn { "Rate limit exceeded for fetchCompleteCv($userId, $cvId). Consider implementing retry logic." }
                    throw RuntimeException("Error from Flowcase API (fetchCompleteCv($userId, $cvId)): ${response.code} - Rate limit exceeded")
                }

                else -> {
                    logger.warn { "Warning: Could not fetch full resume for userId $userId / cvId $cvId. Status: ${response.code}" }
                    throw RuntimeException("Error from Flowcase API (fetchCompleteCv($userId, $cvId)): ${response.code} - ${response.message}")
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