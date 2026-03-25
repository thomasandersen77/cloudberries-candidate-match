package no.cloudberries.ai.port

interface EmbeddingPort {
    val providerName: String
    val modelName: String
    val dimension: Int

    fun isEnabled(): Boolean
    fun embed(text: String): DoubleArray
}
