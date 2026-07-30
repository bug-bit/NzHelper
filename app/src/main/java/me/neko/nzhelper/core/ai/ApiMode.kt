package me.neko.nzhelper.core.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject

sealed class ApiMode(
    val key: String,
    val label: String,
    val chatPath: String,
    val modelsPath: String?,
    val authType: AuthType = AuthType.BEARER
) {
    enum class AuthType { BEARER, X_API_KEY, QUERY_PARAM }

    abstract fun buildRequestBody(
        model: String, systemPrompt: String, userPrompt: String,
        maxTokens: Int, extraFields: JsonObject? = null,
        temperature: Float = 0.7f
    ): String

    object OpenAICompat : ApiMode("openai", "OpenAI", "/chat/completions", "/models", AuthType.BEARER) {
        override fun buildRequestBody(
            model: String, systemPrompt: String, userPrompt: String,
            maxTokens: Int, extraFields: JsonObject?,
            temperature: Float
        ): String = buildOpenAiBody(model, systemPrompt, userPrompt, maxTokens, extraFields, temperature)
    }

    object Google : ApiMode("google", "Google", "/v1/models/__MODEL__:generateContent", "/models", AuthType.QUERY_PARAM) {
        override fun buildRequestBody(
            model: String, systemPrompt: String, userPrompt: String,
            maxTokens: Int, extraFields: JsonObject?,
            temperature: Float
        ): String {
            val body = JsonObject().apply {
                add("contents", JsonArray().apply {
                    add(JsonObject().apply {
                        add("parts", JsonArray().apply {
                            add(JsonObject().apply { addProperty("text", "$systemPrompt\n\n$userPrompt") })
                        })
                    })
                })
                add("generationConfig", JsonObject().apply {
                    addProperty("maxOutputTokens", maxTokens)
                    addProperty("temperature", temperature.toDouble())
                })
            }
            extraFields?.entrySet()?.forEach { (k, v) -> body.add(k, v) }
            return body.toString()
        }
    }

    object Claude : ApiMode("claude", "Claude", "/v1/messages", "/v1/models", AuthType.X_API_KEY) {
        override fun buildRequestBody(
            model: String, systemPrompt: String, userPrompt: String,
            maxTokens: Int, extraFields: JsonObject?,
            temperature: Float
        ): String {
            val body = JsonObject().apply {
                addProperty("model", model)
                addProperty("system", systemPrompt)
                add("messages", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", userPrompt)
                    })
                })
                addProperty("max_tokens", maxTokens)
            }
            extraFields?.entrySet()?.forEach { (k, v) -> body.add(k, v) }
            return body.toString()
        }
    }

    companion object {
        val ALL = listOf(OpenAICompat, Google, Claude)
        fun fromKey(key: String): ApiMode = ALL.firstOrNull { it.key == key } ?: OpenAICompat

        private fun buildOpenAiBody(
            model: String, systemPrompt: String, userPrompt: String,
            maxTokens: Int, extraFields: JsonObject?,
            temperature: Float
        ): String {
            val body = JsonObject().apply {
                addProperty("model", model)
                add("messages", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "system"); addProperty("content", systemPrompt)
                    })
                    add(JsonObject().apply {
                        addProperty("role", "user"); addProperty("content", userPrompt)
                    })
                })
                addProperty("max_tokens", maxTokens)
                addProperty("temperature", temperature)
            }
            extraFields?.entrySet()?.forEach { (k, v) -> body.add(k, v) }
            return body.toString()
        }
    }
}
