package no.cloudberries.candidatematch.integration.flowcase

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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

                FlowcaseUserSearchResponse(
                    mapper.readValue(
                        it,
                        typeRef
                    )
                )
            }
        }
    }

    /**
     * Henter en komplett CV, inkludert en liste av custom_tag_ids.
     */
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
                )
            }
        }
    }

    /**
     * Henter de kategoriserte "custom tags" fra sitt DEDIKERTE masterdata-endepunkt.
     * Dette er den korrekte og eneste metoden som skal hente kategorier.
     */
    fun fetchCustomTagMasterData(): List<CustomTagCategoryDTO> {
        val url = "${config.baseUrl}/v1/masterdata/custom_tags/custom_tag_category" // Korrigert til v1 og riktig sti
        val request = buildGetRequest(url)
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Error fetching custom tag master data: ${response.code} - ${response.message}")
            }
            val responseBody = response.body.string()
            return responseBody.let {
                val typeRef = object : TypeReference<List<CustomTagCategoryDTO>>() {}
                mapper.readValue(
                    it,
                    typeRef
                )
            } ?: emptyList()
        }
    }

    /**
     * Generisk metode for å hente all annen masterdata.
     */
    fun fetchMasterData(section: String, field: String): List<MasterDataEntryDTO> {
        val allEntries = mutableListOf<MasterDataEntryDTO>()
        val url = "${config.baseUrl}/v1/masterdata/$section/$field?include_categories=true"

        val request = buildGetRequest(url).also {
            logger.info { "Fetching master data for $section/$field with url=$url" }
        }

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Error fetching master data for $section/$field: ${response.code} - ${response.message}")
            }

            val responseBody = response.body.string()
            val pageEntries = responseBody.let {
                val typeRef = object : TypeReference<List<MasterDataEntryDTO>>() {}
                mapper.readValue(
                    it,
                    typeRef
                )
            } ?: emptyList()

            allEntries.addAll(pageEntries)
            Thread.sleep(100) // Beholder denne for å være snill mot API-et
        }

        return allEntries
    }

    /**
     * Hent ALLE custom tags for en gitt CV i ett kall.
     * (Dette matcher Flowcase sitt mønster for /v3/cvs/{userId}/{cvId}/custom_tags)
     */
    fun fetchAllCustomTags(userId: String, cvId: String): List<CustomTagDTO> {
        val url = "${config.baseUrl}/v3/cvs/$userId/$cvId/custom_tags"
        val request = buildGetRequest(url).also { logger.info { "Fetching custom tags for cvId=$cvId with url=$url" } }

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.warn { "Could not fetch custom tags for cvId=$cvId (status ${response.code})" }
                return emptyList()
            }
            val responseBody = response.body.string()
            val typeRef = object : TypeReference<List<CustomTagDTO>>() {}
            return mapper.readValue(
                responseBody,
                typeRef
            )
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