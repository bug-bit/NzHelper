package me.neko.nzhelper.core.ai

sealed class AiError(message: String) : Exception(message) {
    data object NotConfigured : AiError("AI 未配置供应商")
    data object NotEnabled : AiError("AI 未启用")
    data object NoActiveProvider : AiError("无激活的供应商")
    data object NoRecentData : AiError("最近7天无记录")
    data object Network : AiError("网络异常，请检查连接")
    data class Http(val code: Int) : AiError("服务器错误 ($code)")
    data class Parse(val raw: String) : AiError("解析失败\n$raw")
    data class Unknown(override val message: String) : AiError(message)
}
