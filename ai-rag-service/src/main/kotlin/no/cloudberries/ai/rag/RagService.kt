package no.cloudberries.ai.rag

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class RagService(
    private val chatClient: ChatClient,
    private val vectorStore: VectorStore
) {

    data class SourceDocument(
        val id: String?,
        val contentPreview: String,
        val metadata: Map<String, Any>
    )

    data class ChatResult(
        val answer: String,
        val sources: List<SourceDocument>
    )

    fun chat(message: String, topK: Int, threshold: Double, filter: String?): ChatResult {
        val searchReqBuilder = SearchRequest.builder()
            .topK(topK)
            .similarityThreshold(threshold)

        // By default, focus on CV documents if no filter is provided
        if (filter.isNullOrBlank()) {
            searchReqBuilder.filterExpression("type == 'cv'")
        } else {
            searchReqBuilder.filterExpression(filter)
        }

        val searchReq = searchReqBuilder.build()

        val qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(searchReq)
            .build()

        val answer = chatClient
            .prompt(message)
            .advisors(qaAdvisor)
            .user { it.text(message) }
            .call()
            .content()

        // Additionally fetch sources explicitly to return with the response
        val docs = vectorStore.similaritySearch(searchReq)
        val sources = docs.map { it.toSourceDocument() }

        return ChatResult(
            answer = answer!!,
            sources = sources
        )
    }

    fun ingestCv(candidateId: Long, name: String?, cvText: String): Int {
        val metadata = linkedMapOf<String, Any>(
            "type" to "cv",
            "candidateId" to candidateId.toString()
        )
        if (!name.isNullOrBlank()) {
            metadata["name"] = name
        }
        val doc = Document(
            cvText,
            metadata
        )
        vectorStore.add(listOf(doc))
        return 1
    }

    private fun Document.toSourceDocument(): SourceDocument = SourceDocument(
        id = this.id,
        contentPreview = text!!.take(200),
        metadata = this.metadata
    )
}
