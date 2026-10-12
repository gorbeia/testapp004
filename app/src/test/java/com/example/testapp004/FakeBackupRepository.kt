package com.example.testapp004

import com.example.testapp004.data.BackupRepository
import com.example.testapp004.data.BackupResult

class FakeBackupRepository : BackupRepository {
    /** Results returned in order; the last one repeats. */
    var backupResults: MutableList<BackupResult> = mutableListOf(BackupResult.Success)
    var restoreResults: MutableList<BackupResult> = mutableListOf(BackupResult.Success)
    var backupCalls = 0
    var restoreCalls = 0

    override suspend fun backup(): BackupResult {
        backupCalls++
        return if (backupResults.size > 1) backupResults.removeAt(0) else backupResults.first()
    }

    override suspend fun restore(): BackupResult {
        restoreCalls++
        return if (restoreResults.size > 1) restoreResults.removeAt(0) else restoreResults.first()
    }
}
