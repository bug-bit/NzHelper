package me.neko.nzhelper.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object AiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun execute(
        baseUrl: String,
        chatPath: String,
        apiKey: String,
        authType: ApiMode.AuthType,
        model: String,
        body: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trimEnd('/')
            val path = chatPath.replace("__MODEL__", model)
            val url = when (authType) {
                ApiMode.AuthType.QUERY_PARAM -> "$cleanBase$path?key=$apiKey"
                else -> "$cleanBase$path"
            }
            val reqBuilder = Request.Builder().url(url)
                .post(body.toRequestBody("application/json".toMediaTypeOrNull()))
                .header("Content-Type", "application/json")
            when (authType) {
                ApiMode.AuthType.BEARER -> reqBuilder.header("Authorization", "Bearer $apiKey")
                ApiMode.AuthType.X_API_KEY -> {
                    reqBuilder.header("x-api-key", apiKey)
                    reqBuilder.header("anthropic-version", "2023-06-01")
                }
                ApiMode.AuthType.QUERY_PARAM -> {}
            }
            extraHeaders.forEach { (k, v) -> reqBuilder.header(k, v) }
            val resp = client.newCall(reqBuilder.build()).execute()
            if (!resp.isSuccessful)
                return@withContext Result.failure(AiError.Http(resp.code))
            Result.success(resp.body.string())
        } catch (e: Exception) {
            Result.failure(AiError.Network)
        }
    }

    suspend fun fetchModels(
        baseUrl: String,
        modelsPath: String?,
        apiKey: String,
        authType: ApiMode.AuthType
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trimEnd('/')
            val path = modelsPath ?: return@withContext Result.success("[]")
            val url = when (authType) {
                ApiMode.AuthType.QUERY_PARAM -> "$cleanBase$path?key=$apiKey"
                else -> "$cleanBase$path"
            }
            val reqBuilder = Request.Builder().url(url)
            when (authType) {
                ApiMode.AuthType.BEARER -> reqBuilder.header("Authorization", "Bearer $apiKey")
                ApiMode.AuthType.X_API_KEY -> {
                    reqBuilder.header("x-api-key", apiKey)
                    reqBuilder.header("anthropic-version", "2023-06-01")
                }
                ApiMode.AuthType.QUERY_PARAM -> {}
            }
            val resp = client.newCall(reqBuilder.build()).execute()
            if (!resp.isSuccessful)
                return@withContext Result.failure(AiError.Http(resp.code))
            Result.success(resp.body.string())
        } catch (e: Exception) {
            Result.failure(AiError.Network)
        }
    }
}
