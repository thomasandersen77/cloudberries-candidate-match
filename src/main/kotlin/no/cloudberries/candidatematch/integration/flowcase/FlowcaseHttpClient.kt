package no.cloudberries.candidatematch.integration.flowcase

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class FlowcaseHttpClient(
    config: FlowcaseConfig
) {
    private val url = "${config.baseUrl}/v2/users/search?page=0&size=10000"
    private val bearerToken = "Bearer ${config.apiKey}"
    private val mapper = jacksonObjectMapper()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    fun fetchAllCvs(): FlowcaseUserSearchResponse {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", bearerToken)
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Error from Flowcase API: ${response.code} - ${response.message}")
            }

            val responseBodyString = response.body?.string()

            return responseBodyString.let { it ->
                val typeRef = object : TypeReference<List<FlowcaseCvDTO>>() {}
                FlowcaseUserSearchResponse(mapper.readValue(it, typeRef))
            }
        }
    }
}