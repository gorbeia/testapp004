package com.example.testapp004.data

import android.content.Intent
import com.example.testapp004.model.AppRelease

interface ApkInstaller {
    suspend fun prepareInstall(release: AppRelease, onProgress: (Float) -> Unit): Intent
}
