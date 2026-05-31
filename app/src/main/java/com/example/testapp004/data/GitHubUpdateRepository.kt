package com.example.testapp004.data

import com.example.testapp004.model.AppRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

class GitHubUpdateRepository @Inject constructor(
    private val client: OkHttpClient,
) : UpdateRepository {
    override suspend fun checkForUpdate(currentVersionName: String): AppRelease? =
        withContext(Dispatchers.IO) {
            try {
                val request =
                    Request.Builder()
                        .url("https://api.github.com/repos/gorbeia/testapp004/releases/latest")
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val tagName = json.getString("tag_name").removePrefix("v")
                if (!isNewer(tagName, currentVersionName)) return@withContext null
                val assets = json.getJSONArray("assets")
                val downloadUrl =
                    (0 until assets.length())
                        .map { assets.getJSONObject(it) }
                        .firstOrNull { it.getString("name").endsWith(".apk") }
                        ?.getString("browser_download_url")
                        ?: return@withContext null
                AppRelease(versionName = tagName, downloadUrl = downloadUrl)
            } catch (e: Exception) {
                null
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
