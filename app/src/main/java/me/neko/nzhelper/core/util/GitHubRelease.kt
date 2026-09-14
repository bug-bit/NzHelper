package me.neko.nzhelper.core.util

import android.util.Log
import com.google.gson.JsonParser
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class GitHubRelease(
    @param:Json(name = "tag_name") val tagName: String,
    @param:Json(name = "name") val releaseName: String
)

data class CiBuild(
    val commitCount: Int,
    val shortHash: String
)

object UpdateChecker {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(GitHubRelease::class.java)

    suspend fun fetchLatestRelease(owner: String, repo: String): GitHubRelease? =
        withContext(Dispatchers.IO) {
            try {
                val resp = client.newCall(
                    Request.Builder()
                        .url("https://api.github.com/repos/$owner/$repo/releases/latest")
                        .header("User-Agent", "MyApp/1.0") // 避免 403
                        .build()
                ).execute()

                if (!resp.isSuccessful) return@withContext null

                val body = resp.body.string().also {
                    Log.d("UpdateCheck", "GitHub raw JSON: $it")
                }

                adapter.fromJson(body)
            } catch (e: Exception) {
                Log.e("UpdateCheck", "Error checking update", e)
                null
            }
        }

    suspend fun fetchLatestCiBuild(owner: String, repo: String): CiBuild? =
        withContext(Dispatchers.IO) {
            try {
                val resp = client.newCall(
                    Request.Builder()
                        .url("https://api.github.com/repos/$owner/$repo/commits?per_page=1")
                        .header("User-Agent", "MyApp/1.0")
                        .build()
                ).execute()

                if (!resp.isSuccessful) return@withContext null

                val body = resp.body.string().also {
                    Log.d("UpdateCheck", "GitHub commits JSON: $it")
                }
                val sha = try {
                    JsonParser.parseString(body).asJsonArray
                        .firstOrNull()?.asJsonObject?.get("sha")?.asString
                } catch (_: Exception) {
                    null
                } ?: return@withContext null

                val commitCount = parseLastPage(resp.header("Link")) ?: 1
                CiBuild(commitCount = commitCount, shortHash = sha.take(7))
            } catch (e: Exception) {
                Log.e("UpdateCheck", "Error checking CI build", e)
                null
            }
        }

    private fun parseLastPage(linkHeader: String?): Int? {
        if (linkHeader.isNullOrBlank()) return null
        val lastPart = linkHeader.split(",").firstOrNull { it.contains("rel=\"last\"") }
            ?: return null
        return Regex("""[?&]page=(\d+)""").find(lastPart)?.groupValues?.get(1)?.toIntOrNull()
    }
}
