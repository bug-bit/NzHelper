package me.neko.nzhelper.core.ai

import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AiModelFetcher : ModelFetcher {

    override suspend fun fetch(
        baseUrl: String,
        apiKey: String,
        mode: ApiMode,
        extraFields: JsonObject?
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val models = mode.modelsPath?.let { path ->
            AiClient.fetchModels(baseUrl, path, apiKey, mode.authType)
                .map { raw -> AiResponseParser.parseModelList(raw, mode) }
                .getOrElse { return@withContext Result.failure(it) }
        } ?: emptyList()

        try {
            val testModel = models.firstOrNull() ?: "gpt-4o-mini"
            val testBody = mode.buildRequestBody(testModel, "", "hi", 1, extraFields, 0.7f)
            AiClient.execute(baseUrl, mode.chatPath, apiKey, mode.authType, testModel, testBody)
                .getOrElse { return@withContext Result.failure(it) }
        } catch (e: Exception) {
            return@withContext Result.failure(AiError.Unknown(e.message ?: "连接测试失败"))
        }

        Result.success(models)
    }
}
