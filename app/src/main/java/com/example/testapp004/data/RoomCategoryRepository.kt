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
        .map { list -> list.map { Category(id = it.id, name = it.name, parentId = it.parentId) } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun addCategory(name: String, parentId: Long?): Long =
        dao.insert(CategoryEntity(name = name, parentId = parentId))

    override suspend fun updateCategory(categoryId: Long, name: String, parentId: Long?) {
        dao.updateById(categoryId, name, parentId)
    }

    override suspend fun deleteCategory(categoryId: Long) {
        // ON DELETE SET NULL on the parent_id FK handles child categories automatically
        dao.deleteById(categoryId)
    }
}
