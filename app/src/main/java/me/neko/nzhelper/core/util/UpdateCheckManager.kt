package me.neko.nzhelper.core.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.neko.nzhelper.BuildConfig
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.core.datastore.UpdateSettings

data class UpdateInfo(
    val tag: String,
    val isCi: Boolean
)

sealed interface UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Failed : UpdateCheckResult
    data object UnsupportedBetaBuild : UpdateCheckResult
}

object UpdateCheckManager {
    private const val OWNER = "bug-bit"
    private const val REPO = "NzHelper"
    private const val CI_CHANNEL_URL = "https://t.me/NzzHelper"

    private var autoCheckStarted = false
    private var manualCheckRequested = false

    val pendingAutoUpdate = MutableStateFlow<UpdateInfo?>(null)

    // 旋转、返回主页都会重建组合，自动检查机会只在进程内消耗一次。
    fun startAutoCheck(context: Context) {
        if (autoCheckStarted) return
        autoCheckStarted = true
        if (!UpdateSettings.isCheckUpdateEnabled(context)) return

        val appContext = context.applicationContext
        NzApplication.appScope.launch {
            when (val result = check(appContext, bypassIgnored = false)) {
                is UpdateCheckResult.Available -> {
                    if (!manualCheckRequested &&
                        UpdateSettings.isCheckUpdateEnabled(appContext)
                    ) {
                        pendingAutoUpdate.value = result.info
                    }
                }

                UpdateCheckResult.Failed -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, "检查更新失败", Toast.LENGTH_SHORT).show()
                    }
                }

                else -> Unit
            }
        }
    }

    fun downloadUrl(info: UpdateInfo): String =
        if (info.isCi) {
            CI_CHANNEL_URL
        } else {
            "https://github.com/$OWNER/$REPO/releases/latest"
        }

    suspend fun checkManually(context: Context): UpdateCheckResult {
        manualCheckRequested = true
        pendingAutoUpdate.value = null
        return check(context, bypassIgnored = true)
    }

    private suspend fun check(context: Context, bypassIgnored: Boolean): UpdateCheckResult {
        val result = if (UpdateSettings.isBetaUpdateEnabled(context)) {
            checkBeta()
        } else {
            checkRelease()
        }

        if (!bypassIgnored &&
            result is UpdateCheckResult.Available &&
            result.info.tag == UpdateSettings.getIgnoredVersion(context)
        ) {
            return UpdateCheckResult.UpToDate
        }
        return result
    }

    private suspend fun checkRelease(): UpdateCheckResult {
        val release = UpdateChecker.fetchLatestRelease(OWNER, REPO)
            ?: return UpdateCheckResult.Failed
        return if (isReleaseNewer(BuildConfig.VERSION_NAME, release.releaseName)) {
            UpdateCheckResult.Available(
                UpdateInfo(tag = release.releaseName, isCi = false)
            )
        } else {
            UpdateCheckResult.UpToDate
        }
    }

    private suspend fun checkBeta(): UpdateCheckResult {
        val ci = UpdateChecker.fetchLatestCiBuild(OWNER, REPO)
            ?: return UpdateCheckResult.Failed
        val local = parseLocalBuild(BuildConfig.VERSION_NAME)
            ?: return UpdateCheckResult.UnsupportedBetaBuild
        return if (isCiNewer(local, ci)) {
            UpdateCheckResult.Available(
                UpdateInfo(
                    tag = "${local.basePrefix}.r${ci.commitCount}.${ci.shortHash}",
                    isCi = true
                )
            )
        } else {
            UpdateCheckResult.UpToDate
        }
    }

    private data class LocalBuild(
        val basePrefix: String,
        val commitCount: Int,
        val shortHash: String
    )

    private fun parseLocalBuild(version: String): LocalBuild? {
        val match = Regex("""(.*)\.r(\d+)\.([0-9a-f]+)$""").matchEntire(version)
            ?: return null
        val count = match.groupValues[2].toIntOrNull() ?: return null
        return LocalBuild(match.groupValues[1], count, match.groupValues[3])
    }

    private fun isCiNewer(local: LocalBuild, remote: CiBuild): Boolean =
        remote.commitCount > local.commitCount ||
                (remote.commitCount == local.commitCount && remote.shortHash != local.shortHash)

    private fun isReleaseNewer(local: String, remote: String): Boolean {
        val localNums = parseNumbers(local)
        val remoteNums = parseNumbers(remote)
        for (i in 0..2) {
            if (remoteNums[i] > localNums[i]) return true
            if (remoteNums[i] < localNums[i]) return false
        }
        return false
    }

    private fun parseNumbers(version: String): List<Int> =
        version.trimStart('v', 'V')
            .substringBefore('-')
            .split('.')
            .map { it.toIntOrNull() ?: 0 }
            .let {
                when {
                    it.size >= 3 -> it.take(3)
                    it.size == 2 -> it + listOf(0)
                    it.size == 1 -> it + listOf(0, 0)
                    else -> listOf(0, 0, 0)
                }
            }
}
