package me.neko.nzhelper.core.ai

data class PresetProvider(
    val key: String,
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
    val mode: ApiMode
) {
    companion object {
        val ALL = listOf(
            PresetProvider(
                key = "openai",
                label = "OpenAI 兼容",
                baseUrl = "",
                defaultModel = "gpt-4o-mini",
                mode = ApiMode.OpenAICompat
            ),
            PresetProvider(
                key = "claude",
                label = "Anthropic 兼容",
                baseUrl = "",
                defaultModel = "claude-3-5-haiku-latest",
                mode = ApiMode.Claude
            )
        )
    }
}
