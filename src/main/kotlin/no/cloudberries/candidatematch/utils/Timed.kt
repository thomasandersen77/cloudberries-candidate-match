package no.cloudberries.candidatematch.utils

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Timed


@Aspect
@Component
class TimerAspect {
    
    private val logger = mu.KotlinLogging.logger {}
    
    @Around("@annotation(no.cloudberries.candidatematch.utils.Timed)")
    fun timeMethod(joinPoint: ProceedingJoinPoint): Any? {
        val start = System.currentTimeMillis()
        val methodName = joinPoint.signature.name
        val className = joinPoint.target::class.simpleName
        
        return try {
            val result = joinPoint.proceed()
            val executionTime = System.currentTimeMillis() - start
            logger.info { "Method ${className}::${methodName} executed in ${executionTime / 1000} seconds" }
            result
        } catch (throwable: Throwable) {
            val executionTime = System.currentTimeMillis() - start
            logger.warn { "Method ${className}::${methodName} failed after ${executionTime / 1000} seconds" }
            throw throwable
        }
    }
}