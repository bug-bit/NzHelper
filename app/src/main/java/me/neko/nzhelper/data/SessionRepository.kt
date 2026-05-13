package me.neko.nzhelper.data

import android.content.Context
import androidx.core.content.edit
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import java.time.LocalDateTime

// 公共数据类
data class Session(
    val timestamp: LocalDateTime?,  // 允许为 null，兼容旧数据
    val duration: Int = 0,
    val remark: String = "",
    val location: String = "",
    val watchedMovie: Boolean = false,
    val climax: Boolean = false,
    val rating: Float = 3f,
    val mood: String = "平静",
    val props: String = "手"
)

// 自定义选项配置
data class CustomOptions(
    val moods: List<String> = listOf("平静", "愉悦", "兴奋", "疲惫", "这是最后一次！"),
    val props: List<String> = listOf("手", "斐济杯", "小胶妻")
)

object SessionRepository {

    private const val PREFS_NAME = "sessions_prefs"
    private const val KEY_SESSIONS = "sessions"
    private const val KEY_CUSTOM_OPTIONS = "custom_options"

    private val gson = NzApplication.gson
    private val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type
    private val customOptionsTypeToken = object : TypeToken<CustomOptions>() {}.type

    suspend fun loadSessions(context: Context): List<Session> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SESSIONS, null)
        if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(json, sessionsTypeToken) as? List<Session> ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                // 如果反序列化失败，返回空列表而不是崩溃
                emptyList()
            }
        }
    }

    suspend fun saveSessions(context: Context, sessions: List<Session>) =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = gson.toJson(sessions)
            prefs.edit { putString(KEY_SESSIONS, json) }
        }

    // 加载自定义选项
    suspend fun loadCustomOptions(context: Context): CustomOptions = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CUSTOM_OPTIONS, null)
        if (json.isNullOrEmpty()) {
            CustomOptions() // 返回默认值
        } else {
            try {
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(json, customOptionsTypeToken) as? CustomOptions ?: CustomOptions()
            } catch (e: Exception) {
                e.printStackTrace()
                CustomOptions() // 如果解析失败，返回默认值
            }
        }
    }

    // 保存自定义选项
    suspend fun saveCustomOptions(context: Context, options: CustomOptions) =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = gson.toJson(options)
            prefs.edit { putString(KEY_CUSTOM_OPTIONS, json) }
        }
}