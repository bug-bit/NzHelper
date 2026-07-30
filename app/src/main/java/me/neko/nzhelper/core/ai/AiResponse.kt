package me.neko.nzhelper.core.ai

data class AiResponse(
    val text: String,
    val reasoning: String? = null,
    val usage: AiUsage? = null
)

data class AiUsage(
    val inputTokens: Int?,
    val outputTokens: Int?
)
