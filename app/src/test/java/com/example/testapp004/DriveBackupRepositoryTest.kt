package com.example.testapp004

import com.example.testapp004.data.BackupLocalStore
import com.example.testapp004.data.BackupRemoteStore
import com.example.testapp004.data.BackupResult
import com.example.testapp004.data.BackupSerializer
import com.example.testapp004.data.BackupSnapshot
import com.example.testapp004.data.ConsentHandle
import com.example.testapp004.data.DriveAuthResult
import com.example.testapp004.data.DriveAuthorizer
import com.example.testapp004.data.DriveBackupRepository
import com.example.testapp004.data.room.AcquaintanceEntity
import com.example.testapp004.data.room.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class FakeAuthorizer(var result: DriveAuthResult = DriveAuthResult.Granted("token")) : DriveAuthorizer {
    override suspend fun authorize() = result
}

private class FakeRemote : BackupRemoteStore {
    var stored: String? = null
    var tokenUsed: String? = null
    var failure: IOException? = null

    override suspend fun upload(accessToken: String, content: String) {
        failure?.let { throw it }
        tokenUsed = accessToken
        stored = content
    }

    override suspend fun download(accessToken: String): String? {
        failure?.let { throw it }
        tokenUsed = accessToken
        return stored
    }
}

private class FakeLocal(var snapshot: BackupSnapshot) : BackupLocalStore {
    var replacedWith: BackupSnapshot? = null

    override suspend fun readSnapshot() = snapshot

    override suspend fun replaceAll(snapshot: BackupSnapshot) {
        replacedWith = snapshot
    }
}

class DriveBackupRepositoryTest {
    private fun snapshot(schemaVersion: Int = AppDatabase.VERSION) =
        BackupSnapshot(
            schemaVersion = schemaVersion,
            createdAt = 1L,
            acquaintances = listOf(AcquaintanceEntity(1, "Ann", "", null, null)),
            categories = emptyList(),
            relations = emptyList(),
            crossRefs = emptyList(),
        )

    private val authorizer = FakeAuthorizer()
    private val remote = FakeRemote()
    private val local = FakeLocal(snapshot())
    private val repository = DriveBackupRepository(authorizer, remote, local)

    @Test
    fun `backup uploads serialized local data with the access token`() = runBlocking {
        assertEquals(BackupResult.Success, repository.backup())
        assertEquals("token", remote.tokenUsed)
        assertEquals(local.snapshot, BackupSerializer.fromJson(remote.stored!!))
    }

    @Test
    fun `backup reports consent required without uploading`() = runBlocking {
        val handle = object : ConsentHandle {}
        authorizer.result = DriveAuthResult.NeedsConsent(handle)
        assertEquals(BackupResult.NeedsConsent(handle), repository.backup())
        assertNull(remote.stored)
    }

    @Test
    fun `backup reports authorization failure`() = runBlocking {
        authorizer.result = DriveAuthResult.Failed("no")
        assertEquals(BackupResult.Failure("no"), repository.backup())
    }

    @Test
    fun `backup reports network failure`() = runBlocking {
        remote.failure = IOException("offline")
        assertEquals(BackupResult.Failure("offline"), repository.backup())
    }

    @Test
    fun `restore replaces local data with downloaded backup`() = runBlocking {
        remote.stored = BackupSerializer.toJson(snapshot())
        assertEquals(BackupResult.Success, repository.restore())
        assertEquals(snapshot(), local.replacedWith)
    }

    @Test
    fun `restore fails when no backup exists and leaves local data untouched`() = runBlocking {
        assertTrue(repository.restore() is BackupResult.Failure)
        assertNull(local.replacedWith)
    }

    @Test
    fun `restore rejects corrupt backup and leaves local data untouched`() = runBlocking {
        remote.stored = "garbage"
        assertTrue(repository.restore() is BackupResult.Failure)
        assertNull(local.replacedWith)
    }

    @Test
    fun `restore rejects backup from newer schema`() = runBlocking {
        remote.stored = BackupSerializer.toJson(snapshot(AppDatabase.VERSION + 1))
        assertTrue(repository.restore() is BackupResult.Failure)
        assertNull(local.replacedWith)
    }
}
