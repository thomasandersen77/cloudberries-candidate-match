package no.cloudberries.ai.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "RAG source attribution")
data class RAGSource(
    val consultantId: String,
    val consultantName: String,
    val chunkId: String,
    val text: String,
    val score: Double,
    val location: String
)
