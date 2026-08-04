package com.example.testapp004

import android.content.Intent
import com.example.testapp004.data.ApkInstaller
import com.example.testapp004.model.AppRelease

class FakeApkInstaller : ApkInstaller {
    var shouldThrow = false
    var progressValues: List<Float> = listOf(0.5f, 1.0f)
    var lastRelease: AppRelease? = null

    override suspend fun prepareInstall(release: AppRelease, onProgress: (Float) -> Unit): Intent {
        lastRelease = release
        if (shouldThrow) throw Exception("Download failed")
        progressValues.forEach { onProgress(it) }
        return Intent()
    }
}
