package no.cloudberries.candidatematch.integration.flowcase

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.cloudberries.candidatematch.utils.flowcaseCvDTOListTypeRef
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class FlowcaseHttpClient(
    val config: FlowcaseConfig
) {
    private val bearerToken = "Bearer ${config.apiKey}"
    private val mapper = jacksonObjectMapper()
    private val client = OkHttpClient.Builder()
        .connectTimeout(
            15,
            TimeUnit.SECONDS
        )
        .build()

    fun fetchAllCvs(): FlowcaseResumeResponse {
        val searchRequest = Request.Builder()
            .url("${config.baseUrl}/v2/users/search?page=0&size=200")
            .header(
                "Authorization",
                bearerToken
            )
            .header(
                "Content-Type",
                "application/json"
            )
            .build()

        client.newCall(searchRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Error from Flowcase API: ${response.code} - ${response.message}")
            }

            val responseBodyString = response.body?.string()
            val flowcaseResumeResponseList = FlowcaseUserSearchResponse(
                mapper.readValue(
                    responseBodyString,
                    flowcaseCvDTOListTypeRef
                )
            )

            val flowcaseFullCvList = mutableListOf<FlowcaseResumeDTO>()
            flowcaseResumeResponseList.flowcaseCvDTOList.forEach {
               flowcaseFullCvList.add(fetchFullCvById(it.userId, it.cvId))
            }
            return FlowcaseResumeResponse(flowcaseFullCvList)

        }
    }

    fun fetchFullCvById(
        userId: String,
        cvId: String
    ): FlowcaseResumeDTO {
        //val url = "${config.baseUrl}/v3/cvs/682c529a17774f004390031f/682c529acf99685aed6fd592"
        val url = "${config.baseUrl}/v3/cvs/$userId/$cvId"
        println("GET: $url")
        val fullCvRequest = Request.Builder()
            .method(
                "GET",
                null
            )
            .url(url)
            .header(
                "Authorization",
                bearerToken
            )
            .header(
                "Content-Type",
                "application/json"
            )
            .build()
        client.newCall(fullCvRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Error from Flowcase API: ${response.code} - ${response.message}")
            }
            val responseBodyString = response.body?.string()
            if (responseBodyString == null) {
                throw RuntimeException("Response body is null")
            }
            return mapper.readValue(responseBodyString, FlowcaseResumeDTO::class.java)
        }
    }
}