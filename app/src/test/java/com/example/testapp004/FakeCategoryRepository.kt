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
        val siblings = _categories.value.filter { it.parentId == parentId }
        val nextSortOrder = (siblings.maxOfOrNull { it.sortOrder } ?: -1) + 1
        _categories.update { it + Category(id = id, name = name, parentId = parentId, sortOrder = nextSortOrder) }
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

    override suspend fun moveCategory(movedId: Long, newParentId: Long?, targetPositionId: Long?) {
        val all = _categories.value
        val moved = all.find { it.id == movedId } ?: return
        val oldParentId = moved.parentId
        if (newParentId == movedId) return
        if (newParentId != null && isDescendant(all, movedId, newParentId)) return

        val newSiblings = all.filter { it.parentId == newParentId && it.id != movedId }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .toMutableList()
        val insertPos = if (targetPositionId != null) {
            newSiblings.indexOfFirst { it.id == targetPositionId }.takeIf { it != -1 } ?: newSiblings.size
        } else {
            newSiblings.size
        }
        newSiblings.add(minOf(insertPos, newSiblings.size), moved)

        val reorderedNew = newSiblings.mapIndexed { i, cat ->
            if (cat.id == movedId) cat.copy(parentId = newParentId, sortOrder = i) else cat.copy(sortOrder = i)
        }
        val reorderedOld = if (oldParentId != newParentId) {
            all.filter { it.parentId == oldParentId && it.id != movedId }
                .sortedWith(compareBy({ it.sortOrder }, { it.id }))
                .mapIndexed { i, cat -> cat.copy(sortOrder = i) }
        } else {
            emptyList()
        }

        val updates = (reorderedNew + reorderedOld).associateBy { it.id }
        _categories.update { list -> list.map { cat -> updates[cat.id] ?: cat } }
    }

    private fun isDescendant(all: List<Category>, ancestor: Long, candidate: Long): Boolean {
        var current: Long? = candidate
        while (current != null) {
            current = all.find { it.id == current }?.parentId
            if (current == ancestor) return true
        }
        return false
    }
}
