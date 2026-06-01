package com.example.testapp004

import com.example.testapp004.util.MainDispatcherRule
import com.example.testapp004.viewmodel.CategoriesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CategoriesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var viewModel: CategoriesViewModel

    @Before
    fun setup() {
        fakeCategoryRepository = FakeCategoryRepository()
        viewModel = CategoriesViewModel(fakeCategoryRepository)
    }

    @Test
    fun `initial state has empty categories list`() {
        assertTrue(viewModel.uiState.value.categories.isEmpty())
    }

    @Test
    fun `initial state has dialog closed`() {
        assertFalse(viewModel.uiState.value.isAddDialogOpen)
    }

    @Test
    fun `openAddDialog sets flag to true`() {
        viewModel.openAddDialog()
        assertTrue(viewModel.uiState.value.isAddDialogOpen)
    }

    @Test
    fun `closeAddDialog sets flag to false`() {
        viewModel.openAddDialog()
        viewModel.closeAddDialog()
        assertFalse(viewModel.uiState.value.isAddDialogOpen)
    }

    @Test
    fun `addCategory creates new category and closes dialog`() {
        viewModel.openAddDialog()
        viewModel.addCategory("Work")
        assertEquals(1, viewModel.uiState.value.categories.size)
        assertEquals("Work", viewModel.uiState.value.categories.first().name)
        assertFalse(viewModel.uiState.value.isAddDialogOpen)
    }

    @Test
    fun `addCategory with blank name does nothing`() {
        viewModel.addCategory("   ")
        assertTrue(viewModel.uiState.value.categories.isEmpty())
    }

    @Test
    fun `addCategory trims whitespace`() {
        viewModel.addCategory("  Family  ")
        assertEquals("Family", viewModel.uiState.value.categories.first().name)
    }

    @Test
    fun `addCategory with parent creates child category`() {
        viewModel.addCategory("Work")
        val parentId = viewModel.uiState.value.categories.first().id
        viewModel.addCategory("Engineering", parentId)
        val child = viewModel.uiState.value.categories.find { it.name == "Engineering" }
        assertNotNull(child)
        assertEquals(parentId, child?.parentId)
    }

    @Test
    fun `addCategory without parent creates root category`() {
        viewModel.addCategory("Family")
        assertNull(viewModel.uiState.value.categories.first().parentId)
    }

    @Test
    fun `deleteCategory removes it from list`() {
        viewModel.addCategory("Work")
        val id = viewModel.uiState.value.categories.first().id
        viewModel.deleteCategory(id)
        assertTrue(viewModel.uiState.value.categories.isEmpty())
    }

    @Test
    fun `deleteCategory only removes the targeted category`() {
        viewModel.addCategory("Work")
        viewModel.addCategory("Family")
        val workId = viewModel.uiState.value.categories.first().id
        viewModel.deleteCategory(workId)
        assertEquals(1, viewModel.uiState.value.categories.size)
        assertEquals("Family", viewModel.uiState.value.categories.first().name)
    }

    @Test
    fun `deleteCategory orphans its children`() {
        viewModel.addCategory("Work")
        val parentId = viewModel.uiState.value.categories.first().id
        viewModel.addCategory("Engineering", parentId)
        viewModel.deleteCategory(parentId)
        val child = viewModel.uiState.value.categories.find { it.name == "Engineering" }
        assertNotNull(child)
        assertNull(child?.parentId)
    }

    @Test
    fun `categories assigned unique ids`() {
        viewModel.addCategory("Work")
        viewModel.addCategory("Family")
        val ids = viewModel.uiState.value.categories.map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `openEditDialog sets editingCategory`() {
        viewModel.addCategory("Work")
        val category = viewModel.uiState.value.categories.first()
        viewModel.openEditDialog(category)
        assertEquals(category, viewModel.uiState.value.editingCategory)
    }

    @Test
    fun `closeEditDialog clears editingCategory`() {
        viewModel.addCategory("Work")
        val category = viewModel.uiState.value.categories.first()
        viewModel.openEditDialog(category)
        viewModel.closeEditDialog()
        assertNull(viewModel.uiState.value.editingCategory)
    }

    @Test
    fun `updateCategory renames the category and closes dialog`() {
        viewModel.addCategory("Work")
        val id = viewModel.uiState.value.categories.first().id
        viewModel.openEditDialog(viewModel.uiState.value.categories.first())
        viewModel.updateCategory(id, "Work & Business", null)
        val updated = viewModel.uiState.value.categories.find { it.id == id }
        assertEquals("Work & Business", updated?.name)
        assertNull(viewModel.uiState.value.editingCategory)
    }

    @Test
    fun `updateCategory trims whitespace`() {
        viewModel.addCategory("Work")
        val id = viewModel.uiState.value.categories.first().id
        viewModel.updateCategory(id, "  Family  ", null)
        assertEquals("Family", viewModel.uiState.value.categories.find { it.id == id }?.name)
    }

    @Test
    fun `updateCategory with blank name does nothing`() {
        viewModel.addCategory("Work")
        val id = viewModel.uiState.value.categories.first().id
        viewModel.openEditDialog(viewModel.uiState.value.categories.first())
        viewModel.updateCategory(id, "   ", null)
        assertEquals("Work", viewModel.uiState.value.categories.find { it.id == id }?.name)
        assertNotNull(viewModel.uiState.value.editingCategory)
    }

    @Test
    fun `updateCategory can change parent`() {
        viewModel.addCategory("Work")
        viewModel.addCategory("Family")
        val workId = viewModel.uiState.value.categories.first { it.name == "Work" }.id
        val familyId = viewModel.uiState.value.categories.first { it.name == "Family" }.id
        viewModel.updateCategory(workId, "Work", familyId)
        assertEquals(familyId, viewModel.uiState.value.categories.find { it.id == workId }?.parentId)
    }

    @Test
    fun `updateCategory can promote to root`() {
        viewModel.addCategory("Work")
        val parentId = viewModel.uiState.value.categories.first().id
        viewModel.addCategory("Engineering", parentId)
        val childId = viewModel.uiState.value.categories.first { it.name == "Engineering" }.id
        viewModel.updateCategory(childId, "Engineering", null)
        assertNull(viewModel.uiState.value.categories.find { it.id == childId }?.parentId)
    }

}
