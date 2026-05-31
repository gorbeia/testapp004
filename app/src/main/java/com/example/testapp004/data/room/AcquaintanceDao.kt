package com.example.testapp004.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AcquaintanceDao {
    @Transaction
    @Query("SELECT * FROM acquaintances ORDER BY name ASC")
    fun getAllWithCategories(): Flow<List<AcquaintanceWithCategories>>

    @Transaction
    @Query("SELECT * FROM acquaintances WHERE id = :id")
    suspend fun getWithCategories(id: Long): AcquaintanceWithCategories?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(acquaintance: AcquaintanceEntity): Long

    @Update
    suspend fun update(acquaintance: AcquaintanceEntity)

    @Query("DELETE FROM acquaintances WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<AcquaintanceCategoryCrossRef>)

    @Query("DELETE FROM acquaintance_categories WHERE acquaintance_id = :acquaintanceId")
    suspend fun deleteCrossRefsForAcquaintance(acquaintanceId: Long)
}
