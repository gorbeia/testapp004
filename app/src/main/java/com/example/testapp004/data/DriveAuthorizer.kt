package com.example.testapp004.data

import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/** Opaque handle the UI uses to ask the user for Drive consent. */
interface ConsentHandle

class PendingIntentConsent(val pendingIntent: PendingIntent) : ConsentHandle

sealed interface DriveAuthResult {
    data class Granted(val accessToken: String) : DriveAuthResult

    data class NeedsConsent(val handle: ConsentHandle) : DriveAuthResult

    data class Failed(val message: String) : DriveAuthResult
}

interface DriveAuthorizer {
    suspend fun authorize(): DriveAuthResult
}

class AndroidDriveAuthorizer @Inject constructor(
    @ApplicationContext private val context: Context,
) : DriveAuthorizer {
    override suspend fun authorize(): DriveAuthResult =
        suspendCancellableCoroutine { continuation ->
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
                .build()
            Identity.getAuthorizationClient(context)
                .authorize(request)
                .addOnSuccessListener { result ->
                    val pendingIntent = result.pendingIntent
                    val token = result.accessToken
                    val outcome = when {
                        result.hasResolution() && pendingIntent != null ->
                            DriveAuthResult.NeedsConsent(PendingIntentConsent(pendingIntent))
                        token != null -> DriveAuthResult.Granted(token)
                        else -> DriveAuthResult.Failed("Google did not return an access token")
                    }
                    continuation.resume(outcome)
                }
                .addOnFailureListener { error ->
                    continuation.resume(DriveAuthResult.Failed(error.message ?: "Google authorization failed"))
                }
        }

    private companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
