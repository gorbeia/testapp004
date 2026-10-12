package com.example.testapp004

import com.example.testapp004.data.BackupResult
import com.example.testapp004.data.ConsentHandle
import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.BackupViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BackupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeBackupRepository
    private lateinit var viewModel: BackupViewModel
    private val consent = object : ConsentHandle {}

    @Before
    fun setup() {
        repository = FakeBackupRepository()
        viewModel = BackupViewModel(repository)
    }

    @Test
    fun `backUp success sets message and clears loading`() {
        viewModel.backUp()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.message)
        assertNull(state.error)
        assertEquals(1, repository.backupCalls)
    }

    @Test
    fun `backUp failure sets error`() {
        repository.backupResults = mutableListOf(BackupResult.Failure("boom"))
        viewModel.backUp()
        assertEquals("boom", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `new action clears previous error`() {
        repository.backupResults = mutableListOf(BackupResult.Failure("boom"), BackupResult.Success)
        viewModel.backUp()
        viewModel.backUp()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `requestRestore shows confirmation without restoring`() {
        viewModel.requestRestore()
        assertTrue(viewModel.uiState.value.showRestoreConfirm)
        assertEquals(0, repository.restoreCalls)
    }

    @Test
    fun `dismissRestore hides confirmation without restoring`() {
        viewModel.requestRestore()
        viewModel.dismissRestore()
        assertFalse(viewModel.uiState.value.showRestoreConfirm)
        assertEquals(0, repository.restoreCalls)
    }

    @Test
    fun `confirmRestore restores and hides confirmation`() {
        viewModel.requestRestore()
        viewModel.confirmRestore()
        val state = viewModel.uiState.value
        assertFalse(state.showRestoreConfirm)
        assertEquals("Backup restored", state.message)
        assertEquals(1, repository.restoreCalls)
    }

    @Test
    fun `consent required exposes request and does not report error`() {
        repository.backupResults = mutableListOf(BackupResult.NeedsConsent(consent))
        viewModel.backUp()
        assertEquals(consent, viewModel.uiState.value.consentRequest)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `granted consent clears request and retries the pending action`() {
        repository.restoreResults = mutableListOf(BackupResult.NeedsConsent(consent), BackupResult.Success)
        viewModel.confirmRestore()
        viewModel.onConsentResult(granted = true)
        assertNull(viewModel.uiState.value.consentRequest)
        assertEquals(2, repository.restoreCalls)
        assertEquals("Backup restored", viewModel.uiState.value.message)
    }

    @Test
    fun `denied consent clears request and sets error without retrying`() {
        repository.backupResults = mutableListOf(BackupResult.NeedsConsent(consent))
        viewModel.backUp()
        viewModel.onConsentResult(granted = false)
        assertNull(viewModel.uiState.value.consentRequest)
        assertNotNull(viewModel.uiState.value.error)
        assertEquals(1, repository.backupCalls)
    }

    @Test
    fun `clearMessages clears message and error`() {
        repository.backupResults = mutableListOf(BackupResult.Failure("boom"))
        viewModel.backUp()
        viewModel.clearMessages()
        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.message)
    }
}
