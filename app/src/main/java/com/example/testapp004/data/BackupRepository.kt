package com.example.testapp004.data

import com.example.testapp004.data.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface BackupResult {
    data object Success : BackupResult

    data class NeedsConsent(val handle: ConsentHandle) : BackupResult

    data class Failure(val message: String) : BackupResult
}

interface BackupRepository {
    /** Uploads a snapshot of all local data to Google Drive. */
    suspend fun backup(): BackupResult

    /** Replaces all local data with the backup stored on Google Drive. */
    suspend fun restore(): BackupResult
}

class DriveBackupRepository @Inject constructor(
    private val authorizer: DriveAuthorizer,
    private val remote: BackupRemoteStore,
    private val local: BackupLocalStore,
) : BackupRepository {
    override suspend fun backup(): BackupResult =
        withToken { token ->
            val json = BackupSerializer.toJson(local.readSnapshot())
            withContext(Dispatchers.IO) { remote.upload(token, json) }
            BackupResult.Success
        }

    override suspend fun restore(): BackupResult =
        withToken { token ->
            val json = withContext(Dispatchers.IO) { remote.download(token) }
                ?: return@withToken BackupResult.Failure("No backup found on Google Drive")
            val snapshot = BackupSerializer.fromJson(json)
            if (snapshot.schemaVersion > AppDatabase.VERSION) {
                return@withToken BackupResult.Failure("This backup was made by a newer version of the app")
            }
            local.replaceAll(snapshot)
            BackupResult.Success
        }

    private suspend fun withToken(block: suspend (String) -> BackupResult): BackupResult =
        when (val auth = authorizer.authorize()) {
            is DriveAuthResult.NeedsConsent -> BackupResult.NeedsConsent(auth.handle)
            is DriveAuthResult.Failed -> BackupResult.Failure(auth.message)
            is DriveAuthResult.Granted ->
                try {
                    block(auth.accessToken)
                } catch (e: InvalidBackupException) {
                    BackupResult.Failure(e.message ?: "Backup file is corrupt")
                } catch (e: java.io.IOException) {
                    BackupResult.Failure(e.message ?: "Network error")
                }
        }
}
