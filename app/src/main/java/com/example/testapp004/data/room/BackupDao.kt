package com.example.testapp004.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BackupDao {
    @Query("SELECT * FROM acquaintances ORDER BY id")
    suspend fun getAcquaintances(): List<AcquaintanceEntity>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM relations ORDER BY id")
    suspend fun getRelations(): List<RelationEntity>

    @Query("SELECT * FROM acquaintance_categories")
    suspend fun getCrossRefs(): List<AcquaintanceCategoryCrossRef>

    @Query("DELETE FROM acquaintance_categories")
    suspend fun deleteCrossRefs()

    @Query("DELETE FROM relations")
    suspend fun deleteRelations()

    @Query("DELETE FROM acquaintances")
    suspend fun deleteAcquaintances()

    @Query("DELETE FROM categories")
    suspend fun deleteCategories()

    @Insert
    suspend fun insertAcquaintances(items: List<AcquaintanceEntity>)

    @Insert
    suspend fun insertCategories(items: List<CategoryEntity>)

    @Insert
    suspend fun insertRelations(items: List<RelationEntity>)

    @Insert
    suspend fun insertCrossRefs(items: List<AcquaintanceCategoryCrossRef>)
}
