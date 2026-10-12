package com.example.testapp004.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.BackupRepository
import com.example.testapp004.data.BackupResult
import com.example.testapp004.data.ConsentHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val consentRequest: ConsentHandle? = null,
    val showRestoreConfirm: Boolean = false,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private enum class Action { BACKUP, RESTORE }

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private var pendingAction: Action? = null

    fun backUp() = run(Action.BACKUP)

    fun requestRestore() {
        _uiState.update { it.copy(showRestoreConfirm = true) }
    }

    fun dismissRestore() {
        _uiState.update { it.copy(showRestoreConfirm = false) }
    }

    fun confirmRestore() {
        _uiState.update { it.copy(showRestoreConfirm = false) }
        run(Action.RESTORE)
    }

    /** Called by the UI after the Google consent screen closes. */
    fun onConsentResult(granted: Boolean) {
        _uiState.update { it.copy(consentRequest = null) }
        val action = pendingAction
        pendingAction = null
        if (granted && action != null) {
            run(action)
        } else {
            _uiState.update { it.copy(error = "Google Drive access was not granted") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, message = null) }
    }

    private fun run(action: Action) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            val result = when (action) {
                Action.BACKUP -> backupRepository.backup()
                Action.RESTORE -> backupRepository.restore()
            }
            pendingAction = if (result is BackupResult.NeedsConsent) action else null
            _uiState.update {
                when (result) {
                    is BackupResult.Success -> it.copy(
                        isLoading = false,
                        message = if (action == Action.BACKUP) "Backup saved to Google Drive" else "Backup restored",
                    )
                    is BackupResult.NeedsConsent -> it.copy(isLoading = false, consentRequest = result.handle)
                    is BackupResult.Failure -> it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}
