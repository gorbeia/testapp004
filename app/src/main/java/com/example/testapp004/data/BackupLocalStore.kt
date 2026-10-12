package com.example.testapp004.data

/** Reads and replaces the whole local data set. */
interface BackupLocalStore {
    suspend fun readSnapshot(): BackupSnapshot

    /** Atomically replaces all local data with [snapshot]. */
    suspend fun replaceAll(snapshot: BackupSnapshot)
}
