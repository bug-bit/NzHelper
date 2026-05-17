package me.neko.nzhelper.ui.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object OplusLiveAlertUtil {
    const val ACTION_ENTER_APP = "enterApp"
    const val ACTION_PAUSE = "me.neko.nzhelper.ACTION_PAUSE"
    const val ACTION_RESUME = "me.neko.nzhelper.ACTION_START"
    const val ACTION_STOP = "me.neko.nzhelper.ACTION_STOP"

    private const val TIMER_ICON = "drawable/timer_logo"
    private const val PAUSE_ICON = "drawable/timer_pause"
    private const val START_ICON = "drawable/timer_start"
    private const val EXIT_ICON = "drawable/timer_exit"
    private const val CAPSULE_TEXT_COLOR = "colorInt/ff5C9DFF"

    fun appConfig(context: Context): String =
        JSONObject()
            .put("isMilestone", true)
            .put("dataSourcePkgName", context.packageName)
            .put("remindType", 0)
            .put("showHostMap", JSONArray().put(16).put(512).put(8))
            .put("lockScreenShowHostMap", JSONArray().put(16))
            .put(
                "panelActionConfigMap",
                JSONObject()
                    .put("100", 1)
                    .put("101", 1)
            )
            .put("immersiveCardType", 2)
            .put("requestLockScreenImmersive", -1)
            .toString()

    fun capsuleData(chronometerBaseMs: Long, running: Boolean, fallbackText: String): String =
        JSONObject()
            .put(
                "capsuleData",
                JSONObject()
                    .put("type", "t_sym")
                    .put("firstIcon", JSONObject().put("Icon", timerIcon()))
                    .put("lastTitle", timerText(chronometerBaseMs, running, fallbackText, false))
            )
            .toString()

    fun cardData(
        chronometerBaseMs: Long,
        running: Boolean,
        fallbackText: String,
        title: String,
    ): String =
        JSONObject()
            .put("primaryData", primaryData(chronometerBaseMs, running, fallbackText, title))
            .put("subData", subData(running))
            .put("criticalData", criticalData(chronometerBaseMs, running, fallbackText, title))
            .put("attr", JSONObject().put("intent", "pi/$ACTION_ENTER_APP"))
            .toString()

    private fun primaryData(
        chronometerBaseMs: Long,
        running: Boolean,
        fallbackText: String,
        title: String,
    ): JSONObject =
        JSONObject()
            .put("type", "t_standard")
            .put("icon", JSONObject().put("Icon", timerIcon(running)))
            .put("style", "TEXT")
            .put("title", timerText(chronometerBaseMs, running, fallbackText, true))
            .put(
                "labels",
                JSONArray().put(
                    JSONArray().put(
                        JSONObject()
                            .put("desc", title)
                            .put("style", "TEXT")
                            .put("text", title)
                            .put("textSize", 14)
                            .put("type", "TEXT")
                    )
                )
            )

    private fun subData(running: Boolean): JSONObject =
        JSONObject()
            .put("type", "t_actions")
            .put(
                "btns",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("desc", if (running) "暂停" else "继续")
                            .put("intent", if (running) "pi/$ACTION_PAUSE" else "pi/$ACTION_RESUME")
                            .put("state", "ON")
                            .put("style", "BUTTON")
                            .put("type", "ICON_LABEL")
                            .put("bgColor", if (running) PAUSE_ICON else START_ICON)
                    )
                    .put(
                        JSONObject()
                            .put("desc", "结束")
                            .put("intent", "pi/$ACTION_STOP")
                            .put("state", "ON")
                            .put("style", "BUTTON")
                            .put("type", "ICON_LABEL")
                            .put("bgColor", EXIT_ICON)
                    )
            )

    private fun criticalData(
        chronometerBaseMs: Long,
        running: Boolean,
        fallbackText: String,
        title: String,
    ): JSONObject =
        JSONObject()
            .put("type", "t_sym_action")
            .put("firstIcon", JSONObject().put("Icon", timerIcon(running)))
            .put("firstTitle", timerText(chronometerBaseMs, running, fallbackText, false))
            .put(
                "firstContent",
                JSONObject()
                    .put("desc", title)
                    .put("text", title)
            )
            .put(
                "lastBtns",
                JSONArray().put(
                    JSONObject()
                        .put("desc", if (running) "暂停" else "继续")
                        .put("intent", if (running) "pi/$ACTION_PAUSE" else "pi/$ACTION_RESUME")
                        .put("state", "ON")
                        .put("style", "BUTTON")
                        .put("type", "ICON_LABEL")
                        .put("bgColor", if (running) PAUSE_ICON else START_ICON)
                )
            )

    private fun timerIcon(animated: Boolean = false): JSONObject =
        JSONObject()
            .put("icon", TIMER_ICON)
            .put("desc", "计时")
            .put("shape", 1)

    private fun timerText(
        chronometerBaseMs: Long,
        running: Boolean,
        fallbackText: String,
        card: Boolean,
    ): JSONObject {
        if (!running) {
            return JSONObject()
                .put("desc", fallbackText)
                .put("text", fallbackText)
                .put("textColor", CAPSULE_TEXT_COLOR)
        }

        return JSONObject()
            .put("when", chronometerBaseMs)
            .put("countDown", false)
            .put("started", true)
            .put("showMs", false)
            .apply {
                if (card) {
                    put("textSize", 26)
                } else {
                    put("textColor", CAPSULE_TEXT_COLOR)
                }
            }
    }
}
