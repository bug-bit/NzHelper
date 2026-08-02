package me.neko.nzhelper.core.ai

import android.content.Context
import androidx.core.content.edit
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.database.AppDatabase
import me.neko.nzhelper.core.database.entity.AiConfigEntity
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
    val manualModels: List<String> = emptyList(),
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

    private const val LEGACY_PREFS = "ai_prefs"
    private const val MIGRATED_KEY = "ai_migrated_to_db"

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

    private fun dao(context: Context) = AppDatabase.get(context).aiConfigDao()

    private suspend fun migrateIfNeeded(context: Context) {
        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        val alreadyMigrated = legacyPrefs.getBoolean(MIGRATED_KEY, false)
        val legacyKeys = legacyPrefs.all.keys.filter { it != MIGRATED_KEY }

        if (alreadyMigrated && legacyKeys.isEmpty()) return

        val dbDao = dao(context)

        if (!alreadyMigrated && legacyKeys.isNotEmpty()) {
            if (dbDao.get(KEY_ENABLED) == null) {
                for (key in legacyKeys) {
                    val str = legacyPrefs.all[key]?.toString() ?: continue
                    dbDao.upsert(AiConfigEntity(key, str))
                }
            }
        }

        if (legacyKeys.isNotEmpty()) {
            legacyPrefs.edit { clear(); putBoolean(MIGRATED_KEY, true) }
        }
    }

    private suspend fun getString(context: Context, key: String, default: String): String {
        migrateIfNeeded(context)
        return dao(context).get(key) ?: default
    }

    private suspend fun getInt(context: Context, key: String, default: Int): Int {
        migrateIfNeeded(context)
        return dao(context).get(key)?.toIntOrNull() ?: default
    }

    private suspend fun getLong(context: Context, key: String, default: Long): Long {
        migrateIfNeeded(context)
        return dao(context).get(key)?.toLongOrNull() ?: default
    }

    private suspend fun getBoolean(context: Context, key: String, default: Boolean): Boolean {
        migrateIfNeeded(context)
        return dao(context).get(key)?.toBooleanStrictOrNull() ?: default
    }

    private suspend fun setString(context: Context, key: String, value: String) {
        migrateIfNeeded(context)
        dao(context).upsert(AiConfigEntity(key, value))
    }

    private suspend fun setInt(context: Context, key: String, value: Int) {
        migrateIfNeeded(context)
        dao(context).upsert(AiConfigEntity(key, value.toString()))
    }

    private suspend fun setLong(context: Context, key: String, value: Long) {
        migrateIfNeeded(context)
        dao(context).upsert(AiConfigEntity(key, value.toString()))
    }

    private suspend fun setBoolean(context: Context, key: String, value: Boolean) {
        migrateIfNeeded(context)
        dao(context).upsert(AiConfigEntity(key, value.toString()))
    }

    // ── 全局启停 ──
    suspend fun isEnabled(context: Context): Boolean =
        getBoolean(context, KEY_ENABLED, false)

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        setBoolean(context, KEY_ENABLED, enabled)
    }

    // ── 提示词设置 ──
    suspend fun getPromptTone(context: Context): String =
        getString(context, KEY_PROMPT_TONE, "warm")

    suspend fun getPromptLength(context: Context): String =
        getString(context, KEY_PROMPT_LENGTH, "medium")

    suspend fun getPromptCustom(context: Context): String =
        getString(context, KEY_PROMPT_CUSTOM, "")

    suspend fun getMaxTokens(context: Context): Int =
        getInt(context, KEY_MAX_TOKENS, 500)

    suspend fun setMaxTokens(context: Context, tokens: Int) {
        setInt(context, KEY_MAX_TOKENS, tokens)
    }

    suspend fun getRefreshIntervalMin(context: Context): Int =
        getInt(context, KEY_REFRESH_INTERVAL, 0)

    suspend fun setRefreshIntervalMin(context: Context, minutes: Int) {
        setInt(context, KEY_REFRESH_INTERVAL, minutes)
    }

    suspend fun getLastRefreshTime(context: Context): Long =
        getLong(context, KEY_LAST_REFRESH, 0L)

    suspend fun setLastRefreshTime(context: Context, time: Long) {
        setLong(context, KEY_LAST_REFRESH, time)
    }

    suspend fun getLastAiText(context: Context): String? {
        migrateIfNeeded(context)
        return dao(context).get(KEY_LAST_AI_TEXT)
    }

    suspend fun getLastAiUsage(context: Context): AiUsage? {
        migrateIfNeeded(context)
        val json = dao(context).get(KEY_LAST_AI_USAGE) ?: return null
        return try {
            gson.fromJson(json, AiUsage::class.java)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveLastAiResponse(context: Context, text: String, usage: AiUsage?) {
        migrateIfNeeded(context)
        val d = dao(context)
        d.upsert(AiConfigEntity(KEY_LAST_AI_TEXT, text))
        if (usage != null) {
            d.upsert(AiConfigEntity(KEY_LAST_AI_USAGE, gson.toJson(usage)))
        } else {
            d.delete(KEY_LAST_AI_USAGE)
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

    suspend fun getDataOptions(context: Context): DataOptions {
        migrateIfNeeded(context)
        val raw = dao(context).get(KEY_DATA_OPTIONS) ?: return DataOptions()
        return try {
            val type = TypeToken.getParameterized(
                Set::class.java, String::class.java
            ).type
            val fields: Set<String> = gson.fromJson(raw, type) ?: emptySet()
            DataOptions(fields)
        } catch (_: Exception) {
            DataOptions()
        }
    }

    suspend fun setDataOptions(context: Context, opts: DataOptions) {
        setString(context, KEY_DATA_OPTIONS, gson.toJson(opts.fields))
    }

    suspend fun getAnalysisDays(context: Context): Int =
        getInt(context, KEY_ANALYSIS_DAYS, 7)

    suspend fun setAnalysisDays(context: Context, days: Int) {
        setInt(context, KEY_ANALYSIS_DAYS, days)
    }

    suspend fun savePrompt(
        context: Context,
        tone: String,
        length: String,
        custom: String
    ) {
        val d = dao(context)
        d.upsert(AiConfigEntity(KEY_PROMPT_TONE, tone))
        d.upsert(AiConfigEntity(KEY_PROMPT_LENGTH, length))
        d.upsert(AiConfigEntity(KEY_PROMPT_CUSTOM, custom))
    }

    // ── 供应商列表 ──
    suspend fun getProviders(context: Context): List<AiProvider> {
        migrateIfNeeded(context)
        val json = dao(context).get(KEY_PROVIDERS) ?: return emptyList()
        return try {
            val type = TypeToken.getParameterized(
                List::class.java, AiProvider::class.java
            ).type
            gson.fromJson<List<AiProvider>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun saveProviders(context: Context, providers: List<AiProvider>) {
        setString(context, KEY_PROVIDERS, gson.toJson(providers))
    }

    suspend fun saveProvider(context: Context, provider: AiProvider) {
        val list = getProviders(context).toMutableList()
        val idx = list.indexOfFirst { it.id == provider.id }
        if (idx >= 0) list[idx] = provider else list += provider
        if (provider.isActive) {
            list.replaceAll { if (it.id != provider.id) it.copy(isActive = false) else it }
        }
        saveProviders(context, list)
    }

    suspend fun deleteProvider(context: Context, id: String) {
        val list = getProviders(context).filter { it.id != id }.toMutableList()
        val hasActive = list.any { it.isActive }
        if (!hasActive && list.isNotEmpty()) {
            list[0] = list[0].copy(isActive = true)
        }
        saveProviders(context, list)
    }

    suspend fun setActive(context: Context, id: String) {
        val list = getProviders(context).map {
            it.copy(isActive = it.id == id)
        }
        saveProviders(context, list)
    }

    suspend fun getActiveProvider(context: Context): AiProvider? =
        getProviders(context).firstOrNull { it.isActive }

    suspend fun isConfigured(context: Context): Boolean =
        getActiveProvider(context)?.isComplete == true
}
