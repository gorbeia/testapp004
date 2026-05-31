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

    override suspend fun deleteCategory(categoryId: Long) {
        _categories.update { list ->
            list.filter { it.id != categoryId }
                .map { if (it.parentId == categoryId) it.copy(parentId = null) else it }
        }
    }
}
