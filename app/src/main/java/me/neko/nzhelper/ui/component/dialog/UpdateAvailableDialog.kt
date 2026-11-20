package me.neko.nzhelper.ui.component.dialog

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Update
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import me.neko.nzhelper.BuildConfig
import me.neko.nzhelper.core.util.UpdateCheckManager
import me.neko.nzhelper.core.util.UpdateInfo

@Composable
fun UpdateAvailableDialog(
    info: UpdateInfo,
    onDismissRequest: () -> Unit,
    onIgnore: () -> Unit
) {
    val context = LocalContext.current
    val actionText = if (info.isCi) {
        "检测到新的 CI 构建，是否前往 Telegram 频道下载？"
    } else {
        "有新版本发布啦，是否前往 GitHub 下载？"
    }

    CustomAppAlertDialog(
        onDismissRequest = onDismissRequest,
        iconVector = Icons.Default.Update,
        title = "检测到新版本",
        message = "当前版本：${BuildConfig.VERSION_NAME}\n" +
                "最新版本：${info.tag}\n\n" +
                actionText,
        confirmText = "去下载",
        confirmIcon = Icons.Default.Download,
        dismissText = "稍后再说",
        onConfirm = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, UpdateCheckManager.downloadUrl(info).toUri())
            )
        },
        tertiaryText = "忽略本次更新",
        onTertiary = onIgnore
    )
}
