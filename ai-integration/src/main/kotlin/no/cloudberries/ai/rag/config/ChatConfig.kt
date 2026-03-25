package no.cloudberries.ai.rag.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "ai.rag", name = ["chat-client.enabled"], havingValue = "true", matchIfMissing = true)
class ChatConfig {
    @Bean("aiRagChatClient")
    fun chatClient(builder: ChatClient.Builder): ChatClient = builder.build()
}
