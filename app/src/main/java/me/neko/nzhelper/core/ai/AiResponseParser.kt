package me.neko.nzhelper.core.ai

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object AiResponseParser {

    private const val TAG = "AiResponseParser"

    fun parse(json: String, mode: ApiMode, compatKey: String? = null): AiResponse? {
        extractApiError(json)?.let { Log.e(TAG, "API error: $it") }
        return when (mode) {
            ApiMode.OpenAICompat -> parseOpenAiCompat(json, compatKey)
            ApiMode.Claude -> parseClaude(json)
        }
    }

    fun parseModelList(json: String, mode: ApiMode): List<String> {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            when (mode) {
                ApiMode.OpenAICompat -> root.getAsJsonArray("data")
                    ?.mapNotNull { it.asJsonObject.get("id")?.asString }
                    ?.sorted() ?: emptyList()
                ApiMode.Claude -> root.getAsJsonArray("data")
                    ?.mapNotNull { it.asJsonObject.get("id")?.asString }
                    ?.sorted() ?: emptyList()
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun extractApiError(json: String): String? {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val err = root.getAsJsonObject("error")
            err?.get("message")?.asString ?: err?.get("code")?.asString
        } catch (_: Exception) { null }
    }

    private fun parseOpenAiCompat(json: String, compatKey: String? = null): AiResponse? {
        return try {
            val root = JsonParser.parseString(json).asJsonObject

            val choices = root.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) {
                Log.w(TAG, "No choices in response: ${json.take(300)}")
                return null
            }
            val first = choices.get(0)?.asJsonObject ?: return null
            val finishReason = first.get("finish_reason")?.asString
            val msg = first.getAsJsonObject("message")
                ?: first.getAsJsonObject("delta") ?: run {
                    Log.w(TAG, "No message/delta in choice: ${first.toString().take(200)}")
                    return null
                }

            val reasoning = msg.get("reasoning_content")?.let { rc ->
                if (rc.isJsonNull) null else rc.asString?.trim()?.takeIf { it.isNotBlank() }
            }

            val usage = root.getAsJsonObject("usage")?.let { u ->
                AiUsage(
                    inputTokens = u.get("prompt_tokens")?.let { if (it.isJsonNull) null else it.asInt },
                    outputTokens = u.get("completion_tokens")?.let { if (it.isJsonNull) null else it.asInt }
                )
            }

            extractContentAsString(msg)?.let { return AiResponse(it, reasoning, usage) }
            extractContentAsArray(msg)?.let { return AiResponse(it, reasoning, usage) }
            msg.get("text")?.let { t ->
                if (!t.isJsonNull) t.asString?.trim()?.takeIf { it.isNotBlank() }
                    ?.let { return AiResponse(it, reasoning = null, usage = usage) }
            }

            if (reasoning != null) {
                val answer = extractAnswerFromReasoning(reasoning)
                if (answer != null) {
                    val hint = if (finishReason == "length")
                        "（回答被截断，建议增大 max_tokens）\n$answer"
                    else answer
                    return AiResponse(hint, reasoning, usage)
                }
            }

            if (finishReason == "length") {
                Log.w(TAG, "Response truncated by token limit, content empty")
            }
            Log.w(TAG, "All extraction strategies failed: ${msg.toString().take(300)}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Parse exception: ${e.message}", e)
            null
        }
    }

    private fun extractAnswerFromReasoning(reasoning: String): String? {
        val instructionKeywords = listOf(
            "分析请求", "评估数据", "制定建议", "角色", "语气", "字数",
            "输出要求", "格式", "限制", "处理数据", "用户数据"
        )
        val structuralPattern = Regex("^\\s*(\\d+[.、]|[-*])\\s*\\*\\*.*\\*\\*")

        val lines = reasoning.lines()
        val candidates = mutableListOf<String>()

        for (i in lines.lastIndex downTo 0) {
            val raw = lines[i].trim()
            if (raw.isBlank()) continue
            if (structuralPattern.matches(raw)) continue
            if (instructionKeywords.any { raw.contains(it) }) continue

            val cleaned = raw.replace(Regex("^\\s*[-*]\\s+"), "")
            if (cleaned.length < 5) continue

            if (cleaned.endsWith("。") || cleaned.endsWith("！") || cleaned.endsWith("？")) {
                return cleaned
            }
            candidates.add(cleaned)
        }

        return candidates.firstOrNull()
    }

    private fun extractContentAsString(msg: JsonObject): String? {
        val content = msg.get("content") ?: return null
        if (content.isJsonNull) return null
        return if (content.isJsonPrimitive) {
            content.asString.trim().takeIf { it.isNotBlank() }
        } else null
    }

    private fun extractContentAsArray(msg: JsonObject): String? {
        val arr = msg.getAsJsonArray("content") ?: return null
        val text = arr.mapNotNull { item ->
            val obj = item.asJsonObject ?: return@mapNotNull null
            val t = obj.get("text") ?: return@mapNotNull null
            if (t.isJsonNull) null else t.asString
        }.joinToString("").trim()
        return text.takeIf { it.isNotBlank() }
    }

    private fun parseClaude(json: String): AiResponse? {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val text = root.getAsJsonArray("content")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString?.trim()?.takeIf { it.isNotBlank() }
                ?: return null
            val usage = root.getAsJsonObject("usage")?.let { u ->
                AiUsage(
                    inputTokens = u.get("input_tokens")?.let { if (it.isJsonNull) null else it.asInt },
                    outputTokens = u.get("output_tokens")?.let { if (it.isJsonNull) null else it.asInt }
                )
            }
            AiResponse(text, usage = usage)
        } catch (_: Exception) { null }
    }
}
