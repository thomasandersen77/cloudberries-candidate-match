package no.cloudberries.candidatematch.service.ai

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class ConversationService(
    private val jdbcTemplate: org.springframework.jdbc.core.JdbcTemplate
) {
    private val logger = KotlinLogging.logger { }
    
    // In-memory storage for conversations (consider Redis or database for production)
    private val conversations = ConcurrentHashMap<String, MutableList<ConversationTurn>>()
    
    data class ConversationTurn(
        val question: String,
        val answer: String,
        val timestamp: LocalDateTime = LocalDateTime.now()
    )
    
    fun addToConversation(conversationId: String, question: String, answer: String) {
        // Persist
        jdbcTemplate.update(
            "INSERT INTO ai_conversation(id) VALUES (?) ON CONFLICT (id) DO NOTHING",
            conversationId
        )
        jdbcTemplate.update(
            "INSERT INTO ai_conversation_turn(conversation_id, question, answer) VALUES (?,?,?)",
            conversationId, question, answer
        )
        jdbcTemplate.update(
            "UPDATE ai_conversation SET updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            conversationId
        )
        logger.debug { "Adding turn to conversation $conversationId" }
        
        val turns = conversations.getOrPut(conversationId) { mutableListOf() }
        turns.add(ConversationTurn(question, answer))
        
        // Keep only the last 10 turns to avoid memory issues
        if (turns.size > 10) {
            turns.removeAt(0)
        }
        
        logger.debug { "Conversation $conversationId now has ${turns.size} turns" }
    }
    
    fun getConversationHistory(conversationId: String): List<ConversationTurn> {
        // Read last 10 turns from DB
        val rows = jdbcTemplate.query(
            "SELECT question, answer, created_at FROM ai_conversation_turn WHERE conversation_id = ? ORDER BY created_at ASC LIMIT 10",
            { rs, _ -> ConversationTurn(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toLocalDateTime()) },
            conversationId
        )
        if (rows.isNotEmpty()) {
            conversations[conversationId] = rows.toMutableList()
            return rows
        }
        return conversations[conversationId]?.toList() ?: emptyList()
    }
    
    fun clearConversation(conversationId: String) {
        logger.debug { "Clearing conversation $conversationId" }
        jdbcTemplate.update("DELETE FROM ai_conversation_turn WHERE conversation_id = ?", conversationId)
        jdbcTemplate.update("DELETE FROM ai_conversation WHERE id = ?", conversationId)
        logger.debug { "Clearing conversation $conversationId" }
        conversations.remove(conversationId)
    }
    
    fun getActiveConversationCount(): Int {
        return conversations.size
    }
    
    @Scheduled(fixedRate = 3600000) // Run every hour
    fun cleanupOldConversations() {
        // Remove from DB older than 24 hours
        jdbcTemplate.update(
            "DELETE FROM ai_conversation WHERE updated_at < (CURRENT_TIMESTAMP - INTERVAL '24 hours')"
        )
        // Remove conversations older than 24 hours
        val cutoff = LocalDateTime.now().minusHours(24)
        val toRemove = mutableListOf<String>()
        
        conversations.forEach { (conversationId, turns) ->
            if (turns.isEmpty() || turns.last().timestamp.isBefore(cutoff)) {
                toRemove.add(conversationId)
            }
        }
        
        toRemove.forEach { conversationId ->
            conversations.remove(conversationId)
            logger.debug { "Removed old conversation $conversationId" }
        }
        
        if (toRemove.isNotEmpty()) {
            logger.info { "Cleaned up ${toRemove.size} old conversations" }
        }
    }
}