package no.cloudberries.candidatematch.integration.flowcase

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class FlowcaseHttpClient(
    val config: FlowcaseConfig
) {
    private val searchUrl = "${config.baseUrl}/v2/users/search?page=0&size=200"
    private val fullCvUrl = "${config.baseUrl}/v2/users/cvs/<user_id>/<cv_id>"
    private val bearerToken = "Bearer ${config.apiKey}"
    private val mapper = jacksonObjectMapper()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    fun fetchAllCvs(): FlowcaseUserSearchResponse {
        val searchRequest = Request.Builder()
            .url(searchUrl)
            .header("Authorization", bearerToken)
            .header("Content-Type", "application/json")
            .build()

        client.newCall(searchRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Error from Flowcase API: ${response.code} - ${response.message}")
            }

            val responseBodyString = response.body?.string()
            println(responseBodyString)
            val flowcaseUserSearchResponseList = responseBodyString.let { it ->
                val typeRef = object : TypeReference<List<FlowcaseCvDTO>>() {}
                FlowcaseUserSearchResponse(mapper.readValue(it, typeRef))
            }

            val flowcaseFullCvList = mutableListOf<FlowcaseCvDTO>()
            flowcaseUserSearchResponseList.flowcaseCvDTOList.forEach {
                if (fetchFullCvById()) return FlowcaseUserSearchResponse(flowcaseFullCvList)
            }
            return FlowcaseUserSearchResponse(flowcaseFullCvList)

        }



    }

    fun fetchFullCvById(
    ): Boolean {
        val url = "${config.baseUrl}/v3/cvs/682c529a17774f004390031f/682c529acf99685aed6fd592"
        println("GET: $url")
        val fullCvRequest = Request.Builder()
            .method("GET", null)
            .url(url)
            .header("Authorization", bearerToken)
            .header("Content-Type", "application/json")
            .build()
        client.newCall(fullCvRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Error from Flowcase API: ${response.code} - ${response.message}")
            }
            val responseBodyString = response.body?.string()


                println(responseBodyString)

        }
        return true

    }
}