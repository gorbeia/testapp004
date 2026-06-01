package com.example.testapp004.data

import com.example.testapp004.data.room.RelationDao
import com.example.testapp004.data.room.RelationEntity
import com.example.testapp004.di.ApplicationScope
import com.example.testapp004.model.Relation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRelationRepository @Inject constructor(
    private val dao: RelationDao,
    @ApplicationScope private val scope: CoroutineScope,
) : RelationRepository {
    override val relations: StateFlow<List<Relation>> = dao.getAll()
        .map { list ->
            list.map {
                Relation(
                    id = it.id,
                    fromId = it.fromId,
                    toId = it.toId,
                    typeKey = it.typeKey,
                    customLabel = it.customLabel,
                )
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun addRelation(fromId: Long, toId: Long, typeKey: String, customLabel: String?) {
        dao.insert(RelationEntity(fromId = fromId, toId = toId, typeKey = typeKey, customLabel = customLabel))
    }

    override suspend fun deleteRelation(relationId: Long) {
        dao.deleteById(relationId)
    }
}
