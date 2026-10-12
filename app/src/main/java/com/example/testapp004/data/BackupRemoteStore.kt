package com.example.testapp004.data

import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

/** Stores a single backup document remotely. */
interface BackupRemoteStore {
    /** Creates or overwrites the backup document. */
    suspend fun upload(accessToken: String, content: String)

    /** Returns the backup document, or null when none exists yet. */
    suspend fun download(accessToken: String): String?
}

/** Keeps the backup as one file in the app-private, hidden Google Drive `appDataFolder`. */
class DriveBackupRemoteStore @Inject constructor(
    private val client: OkHttpClient,
) : BackupRemoteStore {
    override suspend fun upload(accessToken: String, content: String) {
        val existingId = findFileId(accessToken)
        val metadata = JSONObject().put("name", FILE_NAME)
        val request = if (existingId == null) {
            metadata.put("parents", JSONArray().put("appDataFolder"))
            Request.Builder().url("$UPLOAD_URL?uploadType=multipart").post(multipart(metadata, content))
        } else {
            Request.Builder().url("$UPLOAD_URL/$existingId?uploadType=multipart").patch(multipart(metadata, content))
        }
        execute(request.header("Authorization", "Bearer $accessToken").build())
    }

    override suspend fun download(accessToken: String): String? {
        val id = findFileId(accessToken) ?: return null
        val request = Request.Builder()
            .url("$FILES_URL/$id?alt=media")
            .header("Authorization", "Bearer $accessToken")
            .build()
        return execute(request)
    }

    private fun findFileId(accessToken: String): String? {
        val url = FILES_URL.toHttpUrl().newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name = '$FILE_NAME'")
            .addQueryParameter("fields", "files(id)")
            .addQueryParameter("pageSize", "1")
            .build()
        val request = Request.Builder().url(url).header("Authorization", "Bearer $accessToken").build()
        val files = JSONObject(execute(request)).getJSONArray("files")
        return if (files.length() == 0) null else files.getJSONObject(0).getString("id")
    }

    private fun multipart(metadata: JSONObject, content: String): MultipartBody =
        MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(
                Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                metadata.toString().toRequestBody(),
            )
            .addPart(
                Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                content.toRequestBody(),
            )
            .build()

    private fun execute(request: Request): String =
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Google Drive returned HTTP ${response.code}")
            response.body?.string() ?: ""
        }

    private companion object {
        const val FILE_NAME = "testapp004-backup.json"
        const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    }
}
