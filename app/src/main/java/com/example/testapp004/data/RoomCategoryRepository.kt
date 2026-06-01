package com.example.testapp004.data

import com.example.testapp004.data.room.CategoryDao
import com.example.testapp004.data.room.CategoryEntity
import com.example.testapp004.di.ApplicationScope
import com.example.testapp004.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCategoryRepository @Inject constructor(
    private val dao: CategoryDao,
    @ApplicationScope private val scope: CoroutineScope,
) : CategoryRepository {
    override val categories: StateFlow<List<Category>> = dao.getAll()
        .map { list ->
            list.map { Category(id = it.id, name = it.name, parentId = it.parentId, sortOrder = it.sortOrder) }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun addCategory(name: String, parentId: Long?): Long {
        val siblings = dao.getAllOnce().filter { it.parentId == parentId }
        val nextSortOrder = (siblings.maxOfOrNull { it.sortOrder } ?: -1) + 1
        return dao.insert(CategoryEntity(name = name, parentId = parentId, sortOrder = nextSortOrder))
    }

    override suspend fun updateCategory(categoryId: Long, name: String, parentId: Long?) {
        dao.updateById(categoryId, name, parentId)
    }

    override suspend fun deleteCategory(categoryId: Long) {
        // ON DELETE SET NULL on the parent_id FK handles child categories automatically
        dao.deleteById(categoryId)
    }

    override suspend fun reorderCategory(movedId: Long, targetId: Long) {
        val all = dao.getAllOnce()
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

        siblings.forEachIndexed { index, entity ->
            if (entity.sortOrder != index) {
                dao.updateSortOrder(entity.id, index)
            }
        }
    }

    override suspend fun moveCategory(movedId: Long, newParentId: Long?, targetPositionId: Long?) {
        val all = dao.getAllOnce()
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

        if (oldParentId != newParentId) {
            dao.updateById(movedId, moved.name, newParentId)
            val oldSiblings = all.filter { it.parentId == oldParentId && it.id != movedId }
                .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            oldSiblings.forEachIndexed { index, entity -> dao.updateSortOrder(entity.id, index) }
        }
        newSiblings.forEachIndexed { index, entity -> dao.updateSortOrder(entity.id, index) }
    }

    private fun isDescendant(all: List<CategoryEntity>, ancestor: Long, candidate: Long): Boolean {
        var current: Long? = candidate
        while (current != null) {
            current = all.find { it.id == current }?.parentId
            if (current == ancestor) return true
        }
        return false
    }
}
