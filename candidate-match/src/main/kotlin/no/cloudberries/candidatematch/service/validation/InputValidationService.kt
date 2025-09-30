package no.cloudberries.candidatematch.service.validation

import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class InputValidationService {
    
    companion object {
        private const val MAX_TEXT_LENGTH = 10000
        private const val MAX_EMAIL_LENGTH = 255
        private const val MAX_NAME_LENGTH = 200
        
        private val EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
        
        private val POTENTIALLY_DANGEROUS_PATTERNS = listOf(
            Pattern.compile("(?i)<script.*?>.*?</script>"),
            Pattern.compile("(?i)javascript:"),
            Pattern.compile("(?i)on\\w+\\s*="),
            Pattern.compile("\\{\\{.*?\\}\\}"),
            Pattern.compile("<%.*?%>"),
            Pattern.compile("\\$\\{.*?\\}")
        )
    }
    
    fun validateAndSanitizeText(input: String, maxLength: Int = MAX_TEXT_LENGTH): String {
        require(input.isNotBlank()) { "Input cannot be blank" }
        require(input.length <= maxLength) { "Input text too long (max: $maxLength)" }
        
        var sanitized = input.trim()
        
        // Remove potentially dangerous patterns
        POTENTIALLY_DANGEROUS_PATTERNS.forEach { pattern ->
            sanitized = pattern.matcher(sanitized).replaceAll("")
        }
        
        // Replace multiple whitespaces with single space
        sanitized = sanitized.replace(Regex("\\s+"), " ")
        
        return sanitized
    }
    
    fun validateEmail(email: String): String {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(email.length <= MAX_EMAIL_LENGTH) { "Email too long" }
        
        val trimmed = email.trim().lowercase()
        require(EMAIL_PATTERN.matcher(trimmed).matches()) { "Invalid email format" }
        
        return trimmed
    }
    
    fun validateCustomerName(name: String): String {
        require(name.isNotBlank()) { "Customer name cannot be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "Customer name too long" }
        
        // Remove HTML/script tags and potentially dangerous content
        var sanitized = name.trim()
        POTENTIALLY_DANGEROUS_PATTERNS.forEach { pattern ->
            sanitized = pattern.matcher(sanitized).replaceAll("")
        }
        
        return sanitized
    }
    
    fun sanitizeForAIPrompt(text: String): String {
        // Additional sanitization specifically for AI prompts
        var sanitized = validateAndSanitizeText(text)
        
        // Remove prompt injection attempts
        sanitized = sanitized.replace(Regex("(?i)ignore.*(previous|above|prior).*(instruction|prompt|rule)s?"), "")
        sanitized = sanitized.replace(Regex("(?i)new.*(instruction|prompt|rule)s?"), "")
        sanitized = sanitized.replace(Regex("(?i)system.*(message|prompt)"), "")
        
        return sanitized
    }
    
    fun validateSkillNames(skills: List<String>): List<String> {
        return skills.map { skill ->
            require(skill.isNotBlank()) { "Skill name cannot be blank" }
            require(skill.length <= 50) { "Skill name too long" }
            skill.trim().uppercase()
        }.distinct()
    }
}