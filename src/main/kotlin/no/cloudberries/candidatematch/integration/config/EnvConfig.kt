package no.cloudberries.candidatematch.integration.config

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@ConfigurationPropertiesScan
@PropertySource(
    "classpath:application.yaml",
    ignoreResourceNotFound = false,
    encoding = "UTF-8"
)
class EnvConfig