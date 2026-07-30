package me.neko.nzhelper.core.ai

import com.google.gson.JsonObject

interface ModelFetcher {
    suspend fun fetch(
        baseUrl: String,
        apiKey: String,
        mode: ApiMode,
        extraFields: JsonObject? = null
    ): Result<List<String>>
}
