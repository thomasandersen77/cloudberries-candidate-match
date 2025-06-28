package no.cloudberries.candidatematch.integration.flowcase

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException
import kotlin.math.pow
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.time.Instant

class ApiClient(
    private val client: OkHttpClient = OkHttpClient(),
    private val mapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule.Builder().build())
) {
    companion object {
        private const val MAX_RETRIES = 3
        private const val BASE_DELAY = 5000L // milliseconds
    }

    fun makeRequest(
        url: String,
        apiKey: String,
        method: String = "GET",
        payload: Map<String, Any>? = null
    ): List<FlowcaseUser> {
        val headers = Headers.Builder()
            .add("Content-Type", "application/json")
            .add("Authorization", "Bearer $apiKey")
            .build()

        repeat(MAX_RETRIES) { attempt ->
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .headers(headers)

                if (payload != null) {
                    val jsonBody = mapper.writeValueAsString(payload)
                    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                    requestBuilder.method(method, requestBody)
                } else {
                    requestBuilder.method(method, null)
                }

                val request = requestBuilder.build()

                client.newCall(request).execute().use { response ->
                    when (response.code) {
                        429 -> {
                            val delay = (BASE_DELAY * 2.0.pow(attempt)).toLong()
                            println("Rate limited, retry ${attempt + 1}")
                            Thread.sleep(delay)
                            return@use
                        }

                        in 200..299 -> {
                            val json = response.body?.string()
                            // Create a TypeReference to describe the list of users
                            val typeRef = object : TypeReference<List<FlowcaseUser>>() {}
                            return mapper.readValue(json, typeRef)
                        }

                        else -> {
                            response.close()
                            throw IOException("HTTP ${response.code}: ${response.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                if (attempt == MAX_RETRIES - 1) throw e

                val delay = (BASE_DELAY * 2.0.pow(attempt)).toLong()
                Thread.sleep(delay)
            }
        }

        throw IOException("Request failed after $MAX_RETRIES retries")
    }
}

// Example usage
fun main() {
    val apiClient = ApiClient()
    val apiKey = "209dc5eb53a4115dea1bb7d1a9eacbd3"
    val account = "cloudberries"

    try {
        val users = apiClient.makeRequest(
            url = "https://$account.flowcase.com/api/v2/users/search?page=0&size=200",
            apiKey = apiKey
        )
        users.forEach { user ->
            if (user.name == "Thomas Andersen") {
                println(message = "${user.userId}, ${user.defaultCvId}")
            }
        }
    } catch (e: Exception) {
        println("Request failed: $e")
    }
}


/**
 * Represents a single user/consultant object from the Flowcase API.
 * The 'image' field is intentionally omitted as requested.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowcaseUser(

    @JsonProperty("user_id")
    val userId: String,

    @JsonProperty("_id")
    val internalId: String,

    @JsonProperty("id")
    val id: String,

    @JsonProperty("company_id")
    val companyId: String,

    @JsonProperty("company_name")
    val companyName: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("deactivated")
    val deactivated: Boolean,

    @JsonProperty("created_at")
    val createdAt: Instant,

    @JsonProperty("updated_at")
    val updatedAt: Instant,

    @JsonProperty("role")
    val role: String,

    @JsonProperty("roles")
    val roles: List<String>,

    @JsonProperty("office_id")
    val officeId: String,

    @JsonProperty("office_name")
    val officeName: String,

    @JsonProperty("country_id")
    val countryId: String,

    @JsonProperty("country_code")
    val countryCode: String,

    @JsonProperty("language_codes")
    val languageCodes: List<String>,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("telephone")
    val telephone: String?,

    @JsonProperty("title")
    val title: Title?,

    @JsonProperty("default_cv_id")
    val defaultCvId: String

    // The 'image' object is ignored as requested.
)

/**
 * Represents the nested 'title' object which contains language-specific titles.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Title(
    @JsonProperty("int")
    val international: String?,

    @JsonProperty("no")
    val norwegian: String?
)