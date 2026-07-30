package me.neko.nzhelper.core.ai

data class AiConfig(
    val enableThinking: Boolean = false,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 80,
    val extraHeaders: Map<String, String> = emptyMap()
)
