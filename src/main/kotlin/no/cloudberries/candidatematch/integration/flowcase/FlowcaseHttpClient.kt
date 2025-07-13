package no.cloudberries.candidatematch.integration.flowcase

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.cloudberries.candidatematch.utils.flowcaseCvDTOListTypeRef
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class FlowcaseHttpClient(
    private val config: FlowcaseConfig
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val DEFAULT_PAGE_SIZE = 200
        private const val CONTENT_TYPE_HEADER = "Content-Type"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val JSON_CONTENT_TYPE = "application/json"
    }

    private val bearerToken = "Bearer ${config.apiKey}"
    private val mapper = jacksonObjectMapper()
    private val client = OkHttpClient.Builder()
        .connectTimeout(
            CONNECT_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )
        .build()

    fun fetchAllCvs(): FlowcaseResumeResponse {
        val searchRequest = createRequest(
            url = "${config.baseUrl}/v2/users/search?page=0&size=$DEFAULT_PAGE_SIZE"
        )

        client.newCall(searchRequest).execute().use { response ->
            val responseBodyString = handleResponse(response)
            val flowcaseResumeResponseList = FlowcaseUserSearchResponse(
                mapper.readValue(
                    responseBodyString,
                    flowcaseCvDTOListTypeRef
                )
            )

            val flowcaseFullCvList = flowcaseResumeResponseList.flowcaseCvDTOList
                .map {
                    fetchFullCvById(
                        it.userId,
                        it.cvId
                    )
                }

            return FlowcaseResumeResponse(flowcaseFullCvList)
        }
    }

    fun fetchFullCvById(userId: String, cvId: String): FlowcaseResumeDTO {
        val url = "${config.baseUrl}/v3/cvs/$userId/$cvId"
        logger.debug { "Fetching CV from: $url" }

        val fullCvRequest = createRequest(url)

        client.newCall(fullCvRequest).execute().use { response ->
            val responseBodyString = handleResponse(response)
            return mapper.readValue(
                responseBodyString,
                FlowcaseResumeDTO::class.java
            )
        }
    }

    private fun createRequest(url: String, method: String = "GET"): Request =
        Request.Builder()
            .method(
                method,
                null
            )
            .url(url)
            .header(
                AUTHORIZATION_HEADER,
                bearerToken
            )
            .header(
                CONTENT_TYPE_HEADER,
                JSON_CONTENT_TYPE
            )
            .build()

    private fun handleResponse(response: Response): String {
        if (!response.isSuccessful) {
            throw RuntimeException("Flowcase API error (${response.code}): ${response.message}")
        }
        return response.body?.string() ?: throw RuntimeException("Empty response body from Flowcase API")
    }
}