package com.example.testapp004.data

import com.example.testapp004.model.Relation
import kotlinx.coroutines.flow.StateFlow

interface RelationRepository {
    val relations: StateFlow<List<Relation>>

    suspend fun addRelation(fromId: Long, toId: Long, label: String)

    suspend fun deleteRelation(relationId: Long)
}
