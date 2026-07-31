package me.neko.nzhelper.core.ai

sealed class AiError(message: String) : Exception(message) {
    class NotConfigured private constructor() : AiError("AI 未配置供应商") {
        companion object { val INSTANCE = NotConfigured() }
    }
    class NotEnabled private constructor() : AiError("AI 未启用") {
        companion object { val INSTANCE = NotEnabled() }
    }
    class NoActiveProvider private constructor() : AiError("无激活的供应商") {
        companion object { val INSTANCE = NoActiveProvider() }
    }
    class NoRecentData private constructor() : AiError("最近7天无记录") {
        companion object { val INSTANCE = NoRecentData() }
    }
    class Network private constructor() : AiError("网络异常，请检查连接") {
        companion object { val INSTANCE = Network() }
    }
    data class Http(val code: Int, val body: String = "") : AiError("服务器错误 ($code)\n$body")
    data class Parse(val raw: String) : AiError("解析失败\n$raw")
    data class Unknown(override val message: String) : AiError(message)
}
