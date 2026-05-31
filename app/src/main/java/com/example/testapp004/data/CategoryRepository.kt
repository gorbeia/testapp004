package com.example.testapp004.data

import com.example.testapp004.model.Category
import kotlinx.coroutines.flow.StateFlow

interface CategoryRepository {
    val categories: StateFlow<List<Category>>

    suspend fun addCategory(name: String, parentId: Long? = null): Long

    suspend fun updateCategory(category: Category)

    suspend fun deleteCategory(categoryId: Long)
}
