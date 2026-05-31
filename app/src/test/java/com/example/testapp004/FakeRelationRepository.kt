package com.example.testapp004

import com.example.testapp004.data.RelationRepository
import com.example.testapp004.model.Relation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeRelationRepository : RelationRepository {
    private val _relations = MutableStateFlow<List<Relation>>(emptyList())
    override val relations: StateFlow<List<Relation>> = _relations.asStateFlow()
    private var nextId = 4000L

    override suspend fun addRelation(fromId: Long, toId: Long, label: String) {
        val id = nextId++
        _relations.update { it + Relation(id = id, fromId = fromId, toId = toId, label = label) }
    }

    override suspend fun deleteRelation(relationId: Long) {
        _relations.update { list -> list.filter { it.id != relationId } }
    }
}
