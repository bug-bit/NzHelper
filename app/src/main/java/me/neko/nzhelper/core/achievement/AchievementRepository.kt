package me.neko.nzhelper.core.achievement

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.neko.nzhelper.core.achievement.AchievementRepository.newUnlocks
import me.neko.nzhelper.core.database.SessionRepository
import me.neko.nzhelper.core.model.Achievement
import me.neko.nzhelper.core.model.AchievementProgress
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.achievement.util.calculateUnlockTimeline
import me.neko.nzhelper.feature.achievement.util.evaluateAchievements

/**
 * 成就的进度计算与解锁记录。
 *
 * 成就本身由记录数据推导，本地只保存解锁时间与已读状态：一旦解锁就永久保留，
 * 即使后来删除了对应记录也不会丢失。
 */
object AchievementRepository {

    private const val PREFS_NAME = "achievement_prefs"
    private const val KEY_UNLOCKED = "unlocked_at"
    private const val KEY_SEEN = "seen_keys"
    private const val ENTRY_SEPARATOR = ";"
    private const val VALUE_SEPARATOR = '='

    private val syncMutex = Mutex()

    private val _newUnlocks = MutableStateFlow<List<Achievement>>(emptyList())
    private val _progress = MutableStateFlow<List<AchievementProgress>>(emptyList())
    private var syncedWriteCount = -1

    val newUnlocks: StateFlow<List<Achievement>> = _newUnlocks.asStateFlow()

    val progress: StateFlow<List<AchievementProgress>> = _progress.asStateFlow()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readUnlocked(context: Context): LinkedHashMap<String, Long> {
        val raw = prefs(context).getString(KEY_UNLOCKED, null).orEmpty()
        val result = LinkedHashMap<String, Long>()
        raw.split(ENTRY_SEPARATOR).forEach { entry ->
            val index = entry.indexOf(VALUE_SEPARATOR)
            if (index <= 0) return@forEach
            val key = entry.substring(0, index)
            val value = entry.substring(index + 1).toLongOrNull() ?: return@forEach
            result[key] = value
        }
        return result
    }

    private fun writeUnlocked(context: Context, unlocked: Map<String, Long>) {
        val raw = unlocked.entries.joinToString(ENTRY_SEPARATOR) { "${it.key}=${it.value}" }
        prefs(context).edit { putString(KEY_UNLOCKED, raw) }
    }

    private fun readSeen(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SEEN, emptySet())?.toSet() ?: emptySet()

    fun unlockedCount(context: Context): Int = readUnlocked(context).size

    /**
     * 对外可见的成就总数：隐藏成就在解锁前不参与计数，
     * 因此分母会随着隐藏成就被发现而增长。
     */
    fun visibleTotal(context: Context): Int {
        val unlocked = readUnlocked(context).keys
        return Achievement.entries.count { !it.hidden || it.key in unlocked }
    }

    fun unseenCount(context: Context): Int {
        val seen = readSeen(context)
        return readUnlocked(context).keys.count { it !in seen }
    }

    suspend fun markAllSeen(context: Context) = withContext(Dispatchers.IO) {
        markSeen(context, readUnlocked(context).keys)
    }

    private fun markSeen(context: Context, keys: Collection<String>) {
        if (keys.isEmpty()) return
        val seen = readSeen(context) + keys
        prefs(context).edit { putStringSet(KEY_SEEN, seen) }
    }

    /**
     * 重新计算全部成就进度，并把新解锁的成就补齐到本地记录。
     *
     * 由历史数据推导出的成就（例如首次升级后一次性登记）会直接标记为已读，
     * 只有记录保存时新解锁的成就才会产生角标。
     *
     * @param sessions 已加载的记录；为空时自行读取数据库。
     */
    suspend fun sync(
        context: Context,
        sessions: List<Session>? = null
    ): List<AchievementProgress> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val writeCount = SessionRepository.writeCount.value
            if (sessions == null && syncedWriteCount == writeCount && _progress.value.isNotEmpty()) {
                return@withLock _progress.value
            }
            val records = sessions ?: SessionRepository.loadSessions(context)
            val progress = evaluateAndPersist(context, records, markNewAsSeen = true).second
            _progress.value = progress
            syncedWriteCount = SessionRepository.writeCount.value
            progress
        }
    }

    /**
     * 保存记录后调用：解锁新达成的成就并推送到 [newUnlocks]，由界面弹窗展示。
     * 已解锁过的成就不会重复推送。
     */
    suspend fun checkForNewUnlocks(
        context: Context,
        sessions: List<Session>? = null
    ) = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val records = sessions ?: SessionRepository.loadSessions(context)
            val before = readUnlocked(context)
            val progress = evaluateAndPersist(context, records, markNewAsSeen = false).second
            _progress.value = progress
            syncedWriteCount = SessionRepository.writeCount.value
            val fresh = progress
                .filter { it.isUnlocked && it.achievement.key !in before }
                .map { it.achievement }
            if (fresh.isNotEmpty()) {
                _newUnlocks.value = fresh
            }
        }
    }

    private fun evaluateAndPersist(
        context: Context,
        sessions: List<Session>,
        markNewAsSeen: Boolean
    ): Pair<Map<String, Long>, List<AchievementProgress>> {
        val stored = readUnlocked(context)
        val timeline = calculateUnlockTimeline(sessions)

        val merged = LinkedHashMap<String, Long>()
        Achievement.entries.forEach { achievement ->
            val unlockedAt = stored[achievement.key] ?: timeline[achievement]
            if (unlockedAt != null) merged[achievement.key] = unlockedAt
        }
        if (merged != stored) {
            writeUnlocked(context, merged)
            if (markNewAsSeen) markSeen(context, merged.keys - stored.keys)
        }

        return merged to evaluateAchievements(sessions, merged)
    }

    /** 关闭解锁弹窗后调用。 */
    fun consumeNewUnlocks() {
        _newUnlocks.value = emptyList()
    }
}
