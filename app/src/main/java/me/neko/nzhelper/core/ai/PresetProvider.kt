package me.neko.nzhelper.core.ai

import com.google.gson.JsonObject

data class PresetProvider(
    val key: String,
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
    val mode: ApiMode,
    val compatModes: List<CompatMode> = listOf(CompatMode.DEFAULT)
) {
    companion object {
        val ALL = listOf(
            PresetProvider(
                key = "openai",
                label = "OpenAI",
                baseUrl = "https://api.openai.com/v1",
                defaultModel = "gpt-4o-mini",
                mode = ApiMode.OpenAICompat,
                compatModes = listOf(
                    CompatMode("standard", "标准"),
                    CompatMode("deepseek", "DeepSeek", JsonObject().apply {
                        add("thinking", JsonObject().apply { addProperty("type", "disabled") })
                    }),
                    CompatMode("qwen", "通义千问"),
                    CompatMode("moonshot", "Moonshot"),
                    CompatMode("zhipu", "智谱 GLM", JsonObject().apply {
                        add("thinking", JsonObject().apply { addProperty("type", "disabled") })
                    })
                )
            ),
            PresetProvider(
                key = "gemini",
                label = "Gemini",
                baseUrl = "https://generativelanguage.googleapis.com",
                defaultModel = "gemini-2.0-flash",
                mode = ApiMode.Google
            ),
            PresetProvider(
                key = "claude",
                label = "Claude",
                baseUrl = "https://api.anthropic.com",
                defaultModel = "claude-3-5-haiku-latest",
                mode = ApiMode.Claude
            )
        )
    }
}

data class CompatMode(
    val key: String,
    val label: String,
    val extraFields: JsonObject? = null
) {
    companion object {
        val DEFAULT = CompatMode("standard", "标准")
    }
}
