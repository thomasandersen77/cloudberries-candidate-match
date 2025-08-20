package no.cloudberries.candidatematch.service

import mu.KotlinLogging
import no.cloudberries.candidatematch.integration.flowcase.CustomTagDefinitionDTO
import no.cloudberries.candidatematch.integration.flowcase.FlowcaseHttpClient
import no.cloudberries.candidatematch.integration.flowcase.MasterDataEntryDTO
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap


@Service
class FlowcaseAdapterService(
    private val flowcaseHttpClient: FlowcaseHttpClient
) {
    private val logger = KotlinLogging.logger { }

    // Enkel in-memory cache for masterdata. I en produksjonsapplikasjon kan du vurdere en mer robust løsning.
    private val tagMasterDataCache = ConcurrentHashMap<String, CustomTagDefinitionDTO>()
    private val masterDataCache = ConcurrentHashMap<String, MasterDataEntryDTO>()


    /**
     * TODO: Får svært få teknologier fra CV´er fra disse endepunktene. Kommenterer ut @PostConstruct til jeg finner ut hvorfor.
     * Laster inn all masterdata for tags ved oppstart av applikasjonen.
     */
    //@PostConstruct
    fun initializeTagCache() {
        logger.info("Initializing Flowcase tag master data cache...")
        val categories = flowcaseHttpClient.fetchCustomTagMasterData()
        val allTags = categories.flatMap { it.tags }
        allTags.forEach { tag ->
            logger.info(" * Caching tag ${tag.id} - ${tag.valuesNode?.text}")
            tagMasterDataCache[tag.id] = tag
            Thread.sleep(50)
        }

        // Iterer over alle definerte masterdata-typer og hent data
        val masterDataEntries = MasterDataType.entries
        logger.info("Initializing Flowcase master data cache for ${masterDataEntries.size} types...")

        masterDataEntries.forEach { masterDataType ->
            Thread.sleep(50)
            logger.info("Fetching master data for: ${masterDataType.name}...")
            try {
                val entries = flowcaseHttpClient.fetchMasterData(
                    section = masterDataType.endpoint.section,
                    field = masterDataType.endpoint.field
                )
                entries.forEach {
                    logger.info(" * Caching entry ${it.id} - ${it.valuesNode?.text}")
                    masterDataCache[it.id] = it
                }
                logger.info("-> Fetched ${entries.size} entries for ${masterDataType.name}")
            } catch (e: Exception) {
                logger.error("ERROR: Failed to fetch master data for ${masterDataType.name}. Reason: ${e.message}")
            }
        }
        logger.info("Cache initialized with ${tagMasterDataCache.size} tags.")
    }

}

//TODO: Kan antakelig slette klassen og enumen nedenfor hvis jeg ikke trenger masterdata
data class MasterDataEndpoint(val section: String, val field: String)

/**
 * En enum som definerer alle masterdata-typer vi ønsker å hente.
 * Dette gjør det enkelt å legge til nye typer og gir en sentralisert oversikt.
 */
enum class MasterDataType(val endpoint: MasterDataEndpoint) {
    SKILLS(MasterDataEndpoint("technologies", "tags")),
    CATEGORIES(MasterDataEndpoint("technologies", "category")),
    INDUSTRIES(MasterDataEndpoint("project_experiences", "industry")),
    ROLES(MasterDataEndpoint("cv_roles", "name")),
    CERTIFICATIONS(MasterDataEndpoint("certifications", "name")),
    LANGUAGES(MasterDataEndpoint("languages", "name")),
    SCHOOLS(MasterDataEndpoint("educations", "school")),
    DEGREES(MasterDataEndpoint("educations", "degree"));
    // Legg til flere masterdata-typer her etter behov
}