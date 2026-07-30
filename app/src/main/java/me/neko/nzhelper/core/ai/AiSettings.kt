package me.neko.nzhelper.core.ai

import android.content.Context
import androidx.core.content.edit
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.neko.nzhelper.NzApplication
import java.util.UUID.randomUUID

data class AiProvider(
    val id: String = "",
    val name: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val modeKey: String = "openai",
    val model: String = "gpt-4o-mini",
    val isActive: Boolean = false,
    val cachedModels: List<String> = emptyList(),
    val extraFieldsJson: String? = null,
    val compatKey: String? = null,
    val config: AiConfig = AiConfig()
) {
    val isComplete: Boolean
        get() = name.isNotBlank() && apiKey.isNotBlank() && baseUrl.isNotBlank()

    val mode: ApiMode get() = ApiMode.fromKey(modeKey)

    val extraFields: JsonObject?
        get() = extraFieldsJson?.let {
            try {
                JsonParser.parseString(it).asJsonObject
            } catch (_: Exception) {
                null
            }
        }

    companion object {
        fun create(): AiProvider = AiProvider(
            id = randomUUID().toString().take(8)
        )
    }
}

object AiSettings {

    private const val PREFS = "ai_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PROVIDERS = "providers"
    private const val KEY_PROMPT_TONE = "prompt_tone"
    private const val KEY_PROMPT_LENGTH = "prompt_length"
    private const val KEY_PROMPT_CUSTOM = "prompt_custom"
    private const val KEY_MAX_TOKENS = "max_tokens"
    private const val KEY_REFRESH_INTERVAL = "refresh_interval"
    private const val KEY_LAST_REFRESH = "last_ai_refresh"
    private const val KEY_LAST_AI_TEXT = "last_ai_text"
    private const val KEY_LAST_AI_USAGE = "last_ai_usage"
    private const val KEY_DATA_OPTIONS = "ai_data_options"
    private const val KEY_ANALYSIS_DAYS = "analysis_days"

    private val gson = NzApplication.gson

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── 全局启停 ──
    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }

    // ── 提示词设置 ──
    fun getPromptTone(context: Context): String =
        prefs(context).getString(KEY_PROMPT_TONE, "warm") ?: "warm"

    fun getPromptLength(context: Context): String =
        prefs(context).getString(KEY_PROMPT_LENGTH, "medium") ?: "medium"

    fun getPromptCustom(context: Context): String =
        prefs(context).getString(KEY_PROMPT_CUSTOM, "") ?: ""

    fun getMaxTokens(context: Context): Int =
        prefs(context).getInt(KEY_MAX_TOKENS, 500)

    fun setMaxTokens(context: Context, tokens: Int) {
        prefs(context).edit { putInt(KEY_MAX_TOKENS, tokens) }
    }

    fun getRefreshIntervalMin(context: Context): Int =
        prefs(context).getInt(KEY_REFRESH_INTERVAL, 0)

    fun setRefreshIntervalMin(context: Context, minutes: Int) {
        prefs(context).edit { putInt(KEY_REFRESH_INTERVAL, minutes) }
    }

    fun getLastRefreshTime(context: Context): Long =
        prefs(context).getLong(KEY_LAST_REFRESH, 0L)

    fun setLastRefreshTime(context: Context, time: Long) {
        prefs(context).edit { putLong(KEY_LAST_REFRESH, time) }
    }

    fun getLastAiText(context: Context): String? =
        prefs(context).getString(KEY_LAST_AI_TEXT, null)

    fun getLastAiUsage(context: Context): AiUsage? {
        val json = prefs(context).getString(KEY_LAST_AI_USAGE, null) ?: return null
        return try {
            gson.fromJson(json, AiUsage::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun saveLastAiResponse(context: Context, text: String, usage: AiUsage?) {
        prefs(context).edit {
            putString(KEY_LAST_AI_TEXT, text)
            if (usage != null) putString(KEY_LAST_AI_USAGE, gson.toJson(usage))
            else remove(KEY_LAST_AI_USAGE)
        }
    }

    data class DataOptions(
        val fields: Set<String> = emptySet()
    ) {
        fun isEnabled(key: String) = key in fields
    }

    enum class DataField(val key: String, val label: String, val category: String) {
        COUNT("count", "总次数", "基础"),
        DAYS("days", "天数", "基础"),
        TIME_PERIOD("time_period", "时段分布", "基础"),
        MAX_GAP("max_gap", "间隔天数", "基础"),
        AVG_DURATION("avg_duration", "平均时长", "详情"),
        RATING("rating", "评分", "详情"),
        CLIMAX("climax", "高潮", "详情"),
        TAGS("tags", "标签统计", "标签"),
        AGE("age", "年龄", "个人");

        companion object {
            val ALL = entries.toList()
            val GROUPS: Map<String, List<DataField>> = ALL.groupBy { it.category }
        }
    }

    fun getDataOptions(context: Context): DataOptions {
        val raw = try {
            prefs(context).getString(KEY_DATA_OPTIONS, null)
        } catch (_: ClassCastException) {
            null
        } ?: return DataOptions()
        return try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(
                Set::class.java, String::class.java
            ).type
            val fields: Set<String> = gson.fromJson(raw, type) ?: emptySet()
            DataOptions(fields)
        } catch (_: Exception) {
            DataOptions()
        }
    }

    fun setDataOptions(context: Context, opts: DataOptions) {
        prefs(context).edit { putString(KEY_DATA_OPTIONS, gson.toJson(opts.fields)) }
    }

    fun getAnalysisDays(context: Context): Int =
        prefs(context).getInt(KEY_ANALYSIS_DAYS, 7)

    fun setAnalysisDays(context: Context, days: Int) {
        prefs(context).edit { putInt(KEY_ANALYSIS_DAYS, days) }
    }

    fun savePrompt(
        context: Context,
        tone: String,
        length: String,
        custom: String
    ) {
        prefs(context).edit {
            putString(KEY_PROMPT_TONE, tone)
            putString(KEY_PROMPT_LENGTH, length)
            putString(KEY_PROMPT_CUSTOM, custom)
        }
    }

    // ── 供应商列表 ──
    fun getProviders(context: Context): List<AiProvider> {
        val json = prefs(context).getString(KEY_PROVIDERS, null) ?: return emptyList()
        return try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(
                List::class.java, AiProvider::class.java
            ).type
            gson.fromJson<List<AiProvider>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveProviders(context: Context, providers: List<AiProvider>) {
        prefs(context).edit { putString(KEY_PROVIDERS, gson.toJson(providers)) }
    }

    fun saveProvider(context: Context, provider: AiProvider) {
        val list = getProviders(context).toMutableList()
        val idx = list.indexOfFirst { it.id == provider.id }
        if (idx >= 0) list[idx] = provider else list += provider
        if (provider.isActive) {
            list.replaceAll { if (it.id != provider.id) it.copy(isActive = false) else it }
        }
        saveProviders(context, list)
    }

    fun deleteProvider(context: Context, id: String) {
        val list = getProviders(context).filter { it.id != id }.toMutableList()
        val hasActive = list.any { it.isActive }
        if (!hasActive && list.isNotEmpty()) {
            list[0] = list[0].copy(isActive = true)
        }
        saveProviders(context, list)
    }

    fun setActive(context: Context, id: String) {
        val list = getProviders(context).map {
            it.copy(isActive = it.id == id)
        }
        saveProviders(context, list)
    }

    fun getActiveProvider(context: Context): AiProvider? =
        getProviders(context).firstOrNull { it.isActive }

    fun isConfigured(context: Context): Boolean =
        getActiveProvider(context)?.isComplete == true
}
