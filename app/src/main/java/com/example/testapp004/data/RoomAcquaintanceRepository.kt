package com.example.testapp004.data

import androidx.room.withTransaction
import com.example.testapp004.data.room.AcquaintanceCategoryCrossRef
import com.example.testapp004.data.room.AcquaintanceEntity
import com.example.testapp004.data.room.AcquaintanceWithCategories
import com.example.testapp004.data.room.AppDatabase
import com.example.testapp004.di.ApplicationScope
import com.example.testapp004.model.Acquaintance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAcquaintanceRepository @Inject constructor(
    private val db: AppDatabase,
    @ApplicationScope private val scope: CoroutineScope,
) : AcquaintanceRepository {
    private val dao = db.acquaintanceDao()

    override val acquaintances: StateFlow<List<Acquaintance>> = dao.getAllWithCategories()
        .map { list -> list.map(AcquaintanceWithCategories::toDomain) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun addAcquaintance(name: String, bio: String, categoryIds: Set<Long>): Long =
        db.withTransaction {
            val id = dao.insert(AcquaintanceEntity(name = name, bio = bio, androidContactLookupKey = null))
            dao.insertCrossRefs(categoryIds.map { AcquaintanceCategoryCrossRef(id, it) })
            id
        }

    override suspend fun updateAcquaintance(acquaintance: Acquaintance) =
        db.withTransaction {
            dao.update(acquaintance.toEntity())
            dao.deleteCrossRefsForAcquaintance(acquaintance.id)
            dao.insertCrossRefs(acquaintance.categoryIds.map { AcquaintanceCategoryCrossRef(acquaintance.id, it) })
        }

    override suspend fun deleteAcquaintance(acquaintanceId: Long) {
        dao.deleteById(acquaintanceId)
    }

    override suspend fun getAcquaintance(acquaintanceId: Long): Acquaintance? =
        dao.getWithCategories(acquaintanceId)?.toDomain()
}

private fun AcquaintanceWithCategories.toDomain() = Acquaintance(
    id = acquaintance.id,
    name = acquaintance.name,
    bio = acquaintance.bio,
    categoryIds = categories.map { it.id }.toSet(),
    androidContactLookupKey = acquaintance.androidContactLookupKey,
)

private fun Acquaintance.toEntity() = AcquaintanceEntity(
    id = id,
    name = name,
    bio = bio,
    androidContactLookupKey = androidContactLookupKey,
)
