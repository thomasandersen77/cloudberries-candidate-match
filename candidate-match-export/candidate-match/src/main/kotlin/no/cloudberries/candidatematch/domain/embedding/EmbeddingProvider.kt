package no.cloudberries.candidatematch.domain.embedding

interface EmbeddingProvider {
    val providerName: String
    val modelName: String
    val dimension: Int

    fun isEnabled(): Boolean
    fun embed(text: String): DoubleArray
}