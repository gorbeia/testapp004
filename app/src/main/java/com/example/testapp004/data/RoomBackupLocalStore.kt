package com.example.testapp004.data

import androidx.room.withTransaction
import com.example.testapp004.data.room.AppDatabase
import javax.inject.Inject

class RoomBackupLocalStore @Inject constructor(
    private val db: AppDatabase,
) : BackupLocalStore {
    override suspend fun readSnapshot(): BackupSnapshot {
        val dao = db.backupDao()
        return db.withTransaction {
            BackupSnapshot(
                schemaVersion = AppDatabase.VERSION,
                createdAt = System.currentTimeMillis(),
                acquaintances = dao.getAcquaintances(),
                categories = dao.getCategories(),
                relations = dao.getRelations(),
                crossRefs = dao.getCrossRefs(),
            )
        }
    }

    override suspend fun replaceAll(snapshot: BackupSnapshot) {
        val dao = db.backupDao()
        db.withTransaction {
            // Categories reference their parent, so rows may be inserted before the parent exists.
            db.openHelper.writableDatabase.execSQL("PRAGMA defer_foreign_keys = ON")
            dao.deleteCrossRefs()
            dao.deleteRelations()
            dao.deleteAcquaintances()
            dao.deleteCategories()
            dao.insertCategories(snapshot.categories)
            dao.insertAcquaintances(snapshot.acquaintances)
            dao.insertRelations(snapshot.relations)
            dao.insertCrossRefs(snapshot.crossRefs)
        }
    }
}
