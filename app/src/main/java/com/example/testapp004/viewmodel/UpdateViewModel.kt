package com.example.testapp004.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.BuildConfig
import com.example.testapp004.data.ApkInstaller
import com.example.testapp004.data.UpdateRepository
import com.example.testapp004.model.AppRelease
import com.example.testapp004.model.UpdateCheckOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateUiState(
    val isChecking: Boolean = false,
    val updateAvailable: AppRelease? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val error: String? = null,
    val showDebugDialog: Boolean = false,
    val lastCheckOutcome: UpdateCheckOutcome? = null,
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val apkInstaller: ApkInstaller,
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
            _uiState.update { it.copy(isChecking = true, error = null, lastCheckOutcome = null) }
            val outcome = updateRepository.checkForUpdate(BuildConfig.VERSION_NAME)
            val release = if (outcome is UpdateCheckOutcome.UpdateAvailable) outcome.release else null
            val error = when (outcome) {
                is UpdateCheckOutcome.HttpError -> "HTTP ${outcome.code} from server"
                is UpdateCheckOutcome.NetworkError -> outcome.message
                else -> null
            }
            _uiState.update {
                it.copy(
                    isChecking = false,
                    updateAvailable = release,
                    error = error,
                    lastCheckOutcome = outcome,
                )
            }
        }
    }

    fun openDebugDialog() {
        _uiState.update { it.copy(showDebugDialog = true) }
        checkForUpdate()
    }

    fun closeDebugDialog() {
        _uiState.update { it.copy(showDebugDialog = false) }
    }

    fun downloadAndInstall() {
        val release = _uiState.value.updateAvailable ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadProgress = 0f, error = null) }
            try {
                val intent = apkInstaller.prepareInstall(release) { progress ->
                    _uiState.update { it.copy(downloadProgress = progress) }
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
}
