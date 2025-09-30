package no.cloudberries.ai.rag

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class StartupIngestRunner(
    private val dbIngestionService: DbIngestionService,
    @param:Value("\${rag.ingest.on-start:false}") private val onStart: Boolean
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) {
        if (onStart) {
            log.info("rag.ingest.on-start=true -> starting DB ingestion...")
            val report = dbIngestionService.ingestAll()
            log.info(
                "DB ingestion complete: rowsProcessed={}, chunksAdded={}",
                report.rowsProcessed,
                report.chunksAdded
            )
        } else {
            log.info("rag.ingest.on-start is false -> skipping automatic DB ingestion")
        }
    }
}
