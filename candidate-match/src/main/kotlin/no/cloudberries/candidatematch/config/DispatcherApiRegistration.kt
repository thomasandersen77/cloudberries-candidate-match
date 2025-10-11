package no.cloudberries.candidatematch.config

import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.DispatcherServlet

/**
 * Registers an additional servlet mapping so the same Spring DispatcherServlet
 * handles requests under /api/*, effectively making all controllers
 * reachable with and without the /api prefix.
 */
@Configuration
class DispatcherApiRegistration {
    @Bean
    fun apiServletRegistration(dispatcherServlet: DispatcherServlet): ServletRegistrationBean<DispatcherServlet> {
        val bean = ServletRegistrationBean(dispatcherServlet, "/api/*")
        bean.setName("dispatcherServletApi")
        bean.setLoadOnStartup(1)
        return bean
    }
}