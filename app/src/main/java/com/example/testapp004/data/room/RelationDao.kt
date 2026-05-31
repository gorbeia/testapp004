package com.example.testapp004.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationDao {
    @Query("SELECT * FROM relations")
    fun getAll(): Flow<List<RelationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relation: RelationEntity): Long

    @Query("DELETE FROM relations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
