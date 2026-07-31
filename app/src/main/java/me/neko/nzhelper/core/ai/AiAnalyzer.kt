package me.neko.nzhelper.core.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.core.model.Session
import java.time.LocalDateTime

object AiAnalyzer {

    suspend fun fetchModels(
        baseUrl: String,
        apiKey: String,
        mode: ApiMode,
        extraFields: com.google.gson.JsonObject? = null,
        fallbackModel: String = "",
        fetcher: ModelFetcher = AiModelFetcher
    ): Result<List<String>> = fetcher.fetch(baseUrl, apiKey, mode, extraFields, fallbackModel)

    suspend fun analyze(context: Context, sessions: List<Session>): Result<AiResponse> =
        withContext(Dispatchers.IO) {
            if (!AiSettings.isEnabled(context))
                return@withContext Result.failure(AiError.NotEnabled.INSTANCE)
            if (!AiSettings.isConfigured(context))
                return@withContext Result.failure(AiError.NotConfigured.INSTANCE)

            val now = LocalDateTime.now()
            val rangeDays = AiSettings.getAnalysisDays(context)
            val recent = if (rangeDays <= 0) sessions
            else sessions.filter {
                !it.timestamp.isBefore(now.minusDays(rangeDays.toLong())) && !it.timestamp.isAfter(now)
            }
            if (recent.isEmpty())
                return@withContext Result.failure(AiError.NoRecentData.INSTANCE)

            val provider = AiSettings.getActiveProvider(context)
                ?: return@withContext Result.failure(AiError.NoActiveProvider.INSTANCE)
            val mode = provider.mode
            val model = provider.model

            val (systemPrompt, userPrompt) = AiPromptBuilder.build(context, recent, rangeDays)
            val maxTokens = AiSettings.getMaxTokens(context)

            val body = mode.buildRequestBody(
                model, systemPrompt, userPrompt, maxTokens, provider.extraFields,
                provider.config.temperature
            )

            val text = AiClient.execute(
                provider.baseUrl, mode.chatPath, provider.apiKey, mode.authType, model, body,
                provider.config.extraHeaders
            ).getOrElse { return@withContext Result.failure(it) }

            val response = AiResponseParser.parse(text, mode, provider.compatKey)
                ?: return@withContext Result.failure(AiError.Parse(text.take(800)))

            Result.success(response)
        }
}
