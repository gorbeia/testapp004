package com.example.testapp004.model

sealed class UpdateCheckOutcome {
    data class UpdateAvailable(val release: AppRelease) : UpdateCheckOutcome()

    data class UpToDate(val remoteVersion: String, val currentVersion: String) : UpdateCheckOutcome()

    data class NoAsset(val tagName: String) : UpdateCheckOutcome()

    data class HttpError(val code: Int, val url: String) : UpdateCheckOutcome()

    data class NetworkError(val message: String) : UpdateCheckOutcome()
}
