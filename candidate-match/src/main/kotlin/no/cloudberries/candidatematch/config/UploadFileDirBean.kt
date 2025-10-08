package no.cloudberries.candidatematch.config

import com.google.api.client.util.Value
import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

@ConfigurationProperties(prefix = "spring.servlet.multipart")
@Component
data class MultipartProperties(
    val location: String = "/tmp/uploads"
)

@Component
class UploadDirInitializer(
    private val config: MultipartProperties,
    private val fileSystem: FileSystemService
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        fileSystem.ensureDirectoryExists(config.location)
    }
}

interface FileSystemService {
    fun ensureDirectoryExists(path: String)
}

@Component
class DefaultFileSystemService : FileSystemService {
    private val logger = KotlinLogging.logger {}

    override fun ensureDirectoryExists(path: String) {
        try {
            val directory = Paths.get(path)
            Files.createDirectories(directory)
            logger.info { "Upload directory ensured: $directory" }
        } catch (e: IOException) {
            logger.error(e) { "Failed to create upload directory: $path" }
            throw IllegalStateException("Cannot initialize upload directory", e)
        }
    }
}