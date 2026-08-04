package com.example.testapp004.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.testapp004.model.AppRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

class OkHttpApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) : ApkInstaller {
    override suspend fun prepareInstall(release: AppRelease, onProgress: (Float) -> Unit): Intent =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(release.downloadUrl).build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body ?: throw IllegalStateException("Empty response body")
            val contentLength = body.contentLength()
            val dir = File(context.cacheDir, "downloads").apply { mkdirs() }
            val file = File(dir, "update.apk")
            var bytesRead = 0L
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        if (contentLength > 0) onProgress(bytesRead.toFloat() / contentLength)
                    }
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        }
}
