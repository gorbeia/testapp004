package com.example.testapp004

import com.example.testapp004.model.AppRelease
import com.example.testapp004.model.UpdateCheckOutcome
import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.UpdateViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UpdateViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeUpdateRepository: FakeUpdateRepository
    private lateinit var fakeApkInstaller: FakeApkInstaller

    @Before
    fun setup() {
        fakeUpdateRepository = FakeUpdateRepository()
        fakeApkInstaller = FakeApkInstaller()
    }

    private fun createViewModel() = UpdateViewModel(fakeUpdateRepository, fakeApkInstaller)

    @Test
    fun `initial state is not loading after init completes`() {
        val vm = createViewModel()
        assertFalse(vm.uiState.value.isChecking)
    }

    @Test
    fun `checkForUpdate sets updateAvailable when update is available`() {
        val release = AppRelease("2.0.0", "https://example.com/app.apk")
        fakeUpdateRepository.nextOutcome = UpdateCheckOutcome.UpdateAvailable(release)
        val vm = createViewModel()
        assertEquals(release, vm.uiState.value.updateAvailable)
    }

    @Test
    fun `checkForUpdate clears updateAvailable when up to date`() {
        fakeUpdateRepository.nextOutcome = UpdateCheckOutcome.UpToDate("1.0.0", "1.0.0")
        val vm = createViewModel()
        assertNull(vm.uiState.value.updateAvailable)
    }

    @Test
    fun `checkForUpdate sets error on HTTP error`() {
        fakeUpdateRepository.nextOutcome = UpdateCheckOutcome.HttpError(404, "url")
        val vm = createViewModel()
        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.error!!.contains("404"))
    }

    @Test
    fun `checkForUpdate sets error on network error`() {
        fakeUpdateRepository.nextOutcome = UpdateCheckOutcome.NetworkError("timeout")
        val vm = createViewModel()
        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.error!!.contains("timeout"))
    }

    @Test
    fun `checkForUpdate sets lastCheckOutcome`() {
        val outcome = UpdateCheckOutcome.UpToDate("1.0.0", "1.0.0")
        fakeUpdateRepository.nextOutcome = outcome
        val vm = createViewModel()
        assertEquals(outcome, vm.uiState.value.lastCheckOutcome)
    }

    @Test
    fun `openDebugDialog sets showDebugDialog true`() {
        val vm = createViewModel()
        vm.openDebugDialog()
        assertTrue(vm.uiState.value.showDebugDialog)
    }

    @Test
    fun `closeDebugDialog clears showDebugDialog`() {
        val vm = createViewModel()
        vm.openDebugDialog()
        vm.closeDebugDialog()
        assertFalse(vm.uiState.value.showDebugDialog)
    }

    @Test
    fun `dismissUpdate clears updateAvailable and error`() {
        val release = AppRelease("2.0.0", "https://example.com/app.apk")
        fakeUpdateRepository.nextOutcome = UpdateCheckOutcome.UpdateAvailable(release)
        val vm = createViewModel()
        vm.dismissUpdate()
        assertNull(vm.uiState.value.updateAvailable)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `downloadAndInstall sets isDownloading during download`() {
        val release = AppRelease("2.0.0", "https://example.com/app.apk")
        fakeUpdateRepository.nextOutcome = UpdateCheckOutcome.UpdateAvailable(release)
        val vm = createViewModel()
        runBlocking { vm.downloadAndInstall() }
        assertFalse(vm.uiState.value.isDownloading)
    }

    @Test
    fun `downloadAndInstall sets error on failure`() {
        val release = AppRelease("2.0.0", "https://example.com/app.apk")
        fakeUpdateRepository.nextOutcome = UpdateCheckOutcome.UpdateAvailable(release)
        fakeApkInstaller.shouldThrow = true
        val vm = createViewModel()
        runBlocking { vm.downloadAndInstall() }
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isDownloading)
    }

    @Test
    fun `downloadAndInstall does nothing when no update available`() {
        fakeUpdateRepository.nextOutcome = UpdateCheckOutcome.UpToDate("1.0.0", "1.0.0")
        val vm = createViewModel()
        runBlocking { vm.downloadAndInstall() }
        assertNull(fakeApkInstaller.lastRelease)
    }
}
