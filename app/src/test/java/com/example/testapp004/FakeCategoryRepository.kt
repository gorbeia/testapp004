package com.example.testapp004

import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeCategoryRepository : CategoryRepository {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    override val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    private var nextId = 2000L

    override suspend fun addCategory(name: String, parentId: Long?): Long {
        val id = nextId++
        _categories.update { it + Category(id = id, name = name, parentId = parentId) }
        return id
    }

    override suspend fun updateCategory(categoryId: Long, name: String, parentId: Long?) {
        _categories.update { list ->
            list.map { if (it.id == categoryId) it.copy(name = name, parentId = parentId) else it }
        }
    }

    override suspend fun deleteCategory(categoryId: Long) {
        _categories.update { list ->
            list.filter { it.id != categoryId }
                .map { if (it.parentId == categoryId) it.copy(parentId = null) else it }
        }
    }

    override suspend fun reorderCategory(movedId: Long, targetId: Long) {
        val all = _categories.value
        val moved = all.find { it.id == movedId } ?: return
        val target = all.find { it.id == targetId } ?: return
        if (moved.parentId != target.parentId) return

        val siblings = all.filter { it.parentId == moved.parentId }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .toMutableList()

        val movedIdx = siblings.indexOfFirst { it.id == movedId }
        val targetIdx = siblings.indexOfFirst { it.id == targetId }
        if (movedIdx == -1 || targetIdx == -1 || movedIdx == targetIdx) return

        val movedItem = siblings.removeAt(movedIdx)
        siblings.add(minOf(targetIdx, siblings.size), movedItem)

        val reordered = siblings.mapIndexed { index, cat -> cat.copy(sortOrder = index) }
        _categories.update { list ->
            list.map { cat -> reordered.find { it.id == cat.id } ?: cat }
        }
    }
}
