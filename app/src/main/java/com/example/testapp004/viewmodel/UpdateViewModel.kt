package com.example.testapp004.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.BuildConfig
import com.example.testapp004.data.UpdateRepository
import com.example.testapp004.model.AppRelease
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

data class UpdateUiState(
    val isChecking: Boolean = false,
    val updateAvailable: AppRelease? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val error: String? = null,
)

@HiltViewModel
class UpdateViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val updateRepository: UpdateRepository,
        private val okHttpClient: OkHttpClient,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(UpdateUiState())
        val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

        private val _installTrigger = MutableSharedFlow<Intent>()
        val installTrigger: SharedFlow<Intent> = _installTrigger.asSharedFlow()

        init {
            checkForUpdate()
        }

        fun checkForUpdate() {
            viewModelScope.launch {
                _uiState.update { it.copy(isChecking = true, error = null) }
                val release = updateRepository.checkForUpdate(BuildConfig.VERSION_NAME)
                    ?: if (BuildConfig.DEBUG) AppRelease(versionName = "99.0.0", downloadUrl = "") else null
                _uiState.update { it.copy(isChecking = false, updateAvailable = release) }
            }
        }

        fun downloadAndInstall() {
            val release = _uiState.value.updateAvailable ?: return
            if (release.downloadUrl.isEmpty()) return
            viewModelScope.launch {
                _uiState.update { it.copy(isDownloading = true, downloadProgress = 0f, error = null) }
                try {
                    val apkFile =
                        downloadApk(release.downloadUrl) { progress ->
                            _uiState.update { it.copy(downloadProgress = progress) }
                        }
                    val uri =
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            apkFile,
                        )
                    val intent =
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                    _installTrigger.emit(intent)
                    _uiState.update { it.copy(isDownloading = false) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isDownloading = false, error = "Download failed: ${e.message}") }
                }
            }
        }

        fun dismissUpdate() {
            _uiState.update { it.copy(updateAvailable = null, error = null) }
        }

        private suspend fun downloadApk(
            url: String,
            onProgress: (Float) -> Unit,
        ): File =
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(url).build()
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
                            if (contentLength > 0) {
                                onProgress(bytesRead.toFloat() / contentLength)
                            }
                        }
                    }
                }
                file
            }
    }
