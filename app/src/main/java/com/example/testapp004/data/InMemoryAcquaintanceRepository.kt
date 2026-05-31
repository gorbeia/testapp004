package com.example.testapp004.data

import com.example.testapp004.model.Acquaintance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryAcquaintanceRepository @Inject constructor() : AcquaintanceRepository {
    private val _acquaintances = MutableStateFlow<List<Acquaintance>>(emptyList())
    override val acquaintances: StateFlow<List<Acquaintance>> = _acquaintances.asStateFlow()

    override suspend fun addAcquaintance(name: String, bio: String, categoryId: Long?): Long {
        val id = System.currentTimeMillis()
        _acquaintances.update { it + Acquaintance(id = id, name = name, bio = bio, categoryId = categoryId) }
        return id
    }

    override suspend fun updateAcquaintance(acquaintance: Acquaintance) {
        _acquaintances.update { list -> list.map { if (it.id == acquaintance.id) acquaintance else it } }
    }

    override suspend fun deleteAcquaintance(acquaintanceId: Long) {
        _acquaintances.update { list -> list.filter { it.id != acquaintanceId } }
    }

    override suspend fun getAcquaintance(acquaintanceId: Long): Acquaintance? {
        return _acquaintances.value.find { it.id == acquaintanceId }
    }
}
