package com.example.testapp004.data

import com.example.testapp004.BuildConfig
import com.example.testapp004.model.AppRelease
import com.example.testapp004.model.UpdateCheckOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

class GitHubUpdateRepository @Inject constructor(
    private val client: OkHttpClient,
) : UpdateRepository {
    val checkUrl: String get() = if (BuildConfig.DEBUG) {
        "https://api.github.com/repos/gorbeia/testapp004/releases/tags/debug-latest"
    } else {
        "https://api.github.com/repos/gorbeia/testapp004/releases/latest"
    }

    override suspend fun checkForUpdate(currentVersionName: String): UpdateCheckOutcome =
        withContext(Dispatchers.IO) {
            try {
                val url = checkUrl
                val request =
                    Request.Builder()
                        .url(url)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckOutcome.HttpError(response.code, url)
                }
                val body = response.body?.string()
                    ?: return@withContext UpdateCheckOutcome.NetworkError("Empty response body")
                val json = JSONObject(body)
                val assets = json.getJSONArray("assets")
                val apkAsset =
                    (0 until assets.length())
                        .map { assets.getJSONObject(it) }
                        .firstOrNull { it.getString("name").endsWith(".apk") }
                        ?: return@withContext UpdateCheckOutcome.NoAsset(json.optString("tag_name", "unknown"))
                val versionName =
                    if (BuildConfig.DEBUG) {
                        // CI uploads as "app-debug.apk#app-debug-1.0.<run>.apk"; the label carries
                        // the versioned name while name is always the fixed "app-debug.apk"
                        val sourceName = apkAsset.optString("label", "").ifEmpty { apkAsset.getString("name") }
                        sourceName
                            .removePrefix("app-debug-")
                            .removeSuffix(".apk")
                    } else {
                        json.getString("tag_name").removePrefix("v")
                    }
                if (!isNewer(versionName, currentVersionName)) {
                    return@withContext UpdateCheckOutcome.UpToDate(versionName, currentVersionName)
                }
                val downloadUrl = apkAsset.getString("browser_download_url")
                UpdateCheckOutcome.UpdateAvailable(AppRelease(versionName = versionName, downloadUrl = downloadUrl))
            } catch (e: Exception) {
                UpdateCheckOutcome.NetworkError(e.message ?: "Unknown error")
            }
        }

    private fun isNewer(
        remote: String,
        current: String,
    ): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }
}
