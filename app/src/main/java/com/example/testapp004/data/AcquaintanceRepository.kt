package com.example.testapp004.data

import com.example.testapp004.model.Acquaintance
import kotlinx.coroutines.flow.StateFlow

interface AcquaintanceRepository {
    val acquaintances: StateFlow<List<Acquaintance>>

    suspend fun addAcquaintance(name: String, bio: String, categoryId: Long?): Long

    suspend fun updateAcquaintance(acquaintance: Acquaintance)

    suspend fun deleteAcquaintance(acquaintanceId: Long)

    suspend fun getAcquaintance(acquaintanceId: Long): Acquaintance?
}
