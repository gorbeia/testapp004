package com.example.testapp004.data

import com.example.testapp004.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryCategoryRepository @Inject constructor() : CategoryRepository {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    override val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    override suspend fun addCategory(name: String): Long {
        val id = System.currentTimeMillis()
        _categories.update { it + Category(id = id, name = name) }
        return id
    }

    override suspend fun deleteCategory(categoryId: Long) {
        _categories.update { list -> list.filter { it.id != categoryId } }
    }
}
